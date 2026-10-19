import {
  Component,
  OnInit,
  ChangeDetectorRef,
  ElementRef,
  ViewChild,
  AfterViewInit,
  HostListener
} from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { ApiService } from '../../api/services/api.service';
import { CardDeck } from '../../api/models/game.models';
import { environment } from '../../../environments/environment';

function getConfig() {
  return {
    playerCount: Number(localStorage.getItem('playerCount') || 2),
    startingCards: Number(localStorage.getItem('startingCards') || 7),
    withAI: localStorage.getItem('withAI') === 'true',
  };
}

@Component({
  selector: 'app-start-game',
  standalone: true,
  imports: [CommonModule, NgOptimizedImage],
  templateUrl: './start-game.component.html',
  styleUrl: './start-game.component.scss',
})
export class StartGameComponent implements OnInit, AfterViewInit {
  decks: CardDeck[] = [];
  selectedDeckId: string | null = null;
  loading = false;
  error: string | null = null;
  selectedJsonFile: File | null = null;
  selectedImageFiles: File[] = [];
  message = '';
  uploading = false;
  canScrollLeft = false;
  canScrollRight = false;

  @ViewChild('deckScroller', { static: false })
  deckScroller?: ElementRef<HTMLDivElement>;

  constructor(
    private api: ApiService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.loadDecks();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.updateScrollButtons());
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateScrollButtons();
  }

  private loadDecks(): void {
    console.log('Loading decks...');

    this.api.getCardDecks().subscribe({
      next: (decks) => {
        console.log('Decks from backend:', decks);

        this.decks = decks ?? [];

        this.error = null;

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error while loading decks:', err);
        this.decks = [];
        this.error = 'Could not load decks.';

        this.cdr.detectChanges();
      },
    });
  }

  updateScrollButtons(): void {
    const container = this.deckScroller?.nativeElement;
    if (!container) return;

    const maxScrollLeft = container.scrollWidth - container.clientWidth;

    this.canScrollLeft = container.scrollLeft > 0;
    this.canScrollRight = container.scrollLeft < maxScrollLeft - 1;
  }

  scrollDecks(direction: 'left' | 'right') {
    const container = this.deckScroller?.nativeElement;
    if (!container) return;

    const amount = 260;

    container.scrollBy({
      left: direction === 'right' ? amount : -amount,
      behavior: 'smooth',
    });

    setTimeout(() => this.updateScrollButtons(), 250);
  }

  onDeckScroll(): void {
    this.updateScrollButtons();
  }

  selectDeck(id: string) {
    this.selectedDeckId = id;
  }

  startGame() {
    if (!this.selectedDeckId) return;
    const config = getConfig();
    this.loading = true;
    this.error = null;

    this.api
      .createGame(this.selectedDeckId, config.playerCount, config.startingCards, config.withAI)
      .subscribe({
        next: (res) => {
          console.log('CREATE GAME RESPONSE:', res);
          localStorage.setItem('gameId', res.gameId);
          localStorage.setItem('selectedDeckId', this.selectedDeckId!);

          localStorage.setItem(
            'players',
            JSON.stringify(res.players)
          );

          void this.router.navigate(['/active-game']);
        },
        error: () => {
          this.loading = false;
          this.error = 'Could not start game. Is the backend running?';
        },
      });
  }

  onJsonSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedJsonFile = input.files?.[0] ?? null;
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedImageFiles = input.files ? Array.from(input.files) : [];
  }
  uploadDeck(): void {
    if (this.uploading) return;

    if (!this.selectedJsonFile) {
      this.message = 'Please select a JSON file.';
      return;
    }

    if (this.selectedImageFiles.length === 0) {
      this.message = 'Please select card images.';
      return;
    }

    this.uploading = true;
    this.message = 'Uploading...';

    this.api.uploadDeck(this.selectedJsonFile, this.selectedImageFiles).subscribe({
      next: (response) => {
        this.message = `Upload successful: ${response.deckName}`;
        this.uploading = false;
        this.loadDecks();
        this.cdr.detectChanges();
        console.log(response);
      },
      error: (error) => {
        console.error(error);

        const headerMessage = error.headers?.get('X-Error-Message');

        if (headerMessage) {
          this.message = headerMessage;
        } else if (error.error?.errorMessage) {
          this.message = error.error.errorMessage;
        } else if (typeof error.error === 'string') {
          this.message = error.error;
        } else {
          this.message = 'Upload failed.';
        }

        this.uploading = false;
        this.cdr.detectChanges();
      },
    });
  }

  downloadJsonExample(): void {
    const link = document.createElement('a');
    link.href = '/examples/deck-example.json';
    link.download = 'deck-example.json';
    link.click();
  }

  downloadActionCards(): void {
    const link = document.createElement('a');
    link.href = '/examples/action-cards.zip';
    link.download = 'action-cards.zip';
    link.click();
  }

  deckImageUrl(deckId: string, imageName: string): string {
    return `${environment.apiUrl}/deck-images/${deckId}/${imageName}`;
  }

}
