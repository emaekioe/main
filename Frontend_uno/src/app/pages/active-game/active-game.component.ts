import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  HostListener,
  ChangeDetectorRef,
  ElementRef,
  ViewChild,
  NgZone,
} from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Router } from '@angular/router';
import { AiAction, CardInstance, GameState, PlayerState } from '../../api/models/game.models';
import { GameRulesComponent } from '../game-rules/game-rules.component';
import { ApiService } from '../../api/services/api.service';
import { environment } from '../../../environments/environment';

type ResizeDirection = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw';
type PlayColor = 'red' | 'blue' | 'green' | 'yellow';
type DisplayPlayer = {
  id: string;
  name: string;
  position: number;
  isRealId: boolean;
};

@Component({
  selector: 'app-active-game',
  standalone: true,
  imports: [CommonModule, GameRulesComponent, NgOptimizedImage],
  templateUrl: './active-game.component.html',
  styleUrl: './active-game.component.scss',
})
export class ActiveGameComponent implements OnInit, OnDestroy, AfterViewInit {
  gameState!: GameState;
  hand: CardInstance[] = [];
  visibleCurrentPlayerId: string | null = null;
  visibleNextPlayerId: string | null = null;
  selectedCard: CardInstance | null = null;
  error: string | null = null;
  previewCard: CardInstance | null = null;
  private handCardClickTimer: ReturnType<typeof setTimeout> | null = null;
  private topCardClickTimer: ReturnType<typeof setTimeout> | null = null;
  message: string | null = null;
  loading = false;
  gameFinished = false;
  showRules = false;
  showPlayers = false;
  showResultInput = false;
  resultInput: number | null = null;
  showUnoButton = false;
  unoClicked = false;
  deckId = localStorage.getItem('selectedDeckId') || '';

  withAI = localStorage.getItem('withAI') === 'true';
  aiPlayerId: string | null = null;
  showAiTurn = false;
  aiTurnMessage = '';

  currentPlayerIndex = 0;
  showPassScreen = false;
  pendingNextPlayerIndex: number | null = null;
  isInitialPassScreen = false;
  turnTransitionPending = false;
  playerHands: Record<string, CardInstance[]> = {};
  showExitConfirm = false;
  skippedPlayerId: string | null = null;
  passScreenInfo: string | null = null;
  receivedCardMessages: Record<string, string> = {};
  receivedCardCards: Record<string, CardInstance> = {};
  receivedCardPreview: CardInstance | null = null;
  readonly turnPreviewDelayMs = 1500;
  private pendingStateAfterUno: GameState | null = null;

  @ViewChild('playArea') playAreaRef!: ElementRef<HTMLElement>;
  @ViewChild('handCards') handCardsRef!: ElementRef<HTMLElement>;
  @ViewChild('resultInputField') resultInputFieldRef?: ElementRef<HTMLInputElement>;

  handCardStep = 12;
  topOpponentCardStep = 2;
  leftOpponentCardMargin = -20;
  rightOpponentCardMargin = -20;

  private resizeObserver: ResizeObserver | null = null;

  private unoTimer: ReturnType<typeof setTimeout> | null = null;
  readonly unoTimeLimitMs = 5000;

  playerNames: string[] = JSON.parse(
    localStorage.getItem('playerNames') || '["Player 1", "Player 2", "Player 3", "Player 4"]',
  );

  private getPlayerTokenById(playerId: string): string | null {
    const players = JSON.parse(localStorage.getItem('players') || '[]');

    const player = players.find((p: any) => p.playerId === playerId);

    return player?.playerToken ?? null;
  }

  private getCurrentPlayerToken(): string | null {
    const token = this.getPlayerTokenById(this.gameState.currentPlayerId);

    if (!token) {
      console.error('No token found');
      return null;
    }

    return token;
  }

  private finishGame(winnerPlayerId: string): void {
    this.gameFinished = true;
    this.showUnoButton = false;
    this.showResultInput = false;
    this.showColorSelection = false;
    this.showMinusOneSelection = false;
    this.showPassScreen = false;
    this.showAiTurn = false;
    this.aiTurnMessage = '';
    this.turnTransitionPending = false;
    this.pendingNextPlayerIndex = null;
    this.selectedCard = null;
    this.loading = false;

    if (this.unoTimer) {
      clearTimeout(this.unoTimer);
      this.unoTimer = null;
    }

    this.message = `${this.displayPlayerName(winnerPlayerId)} won the game!`;
    this.cdr.detectChanges();
  }

  private readonly defaultPopup = {
    width: 650,
    height: 450,
  };

  popup = {
    left: 0,
    top: 0,
    width: this.defaultPopup.width,
    height: this.defaultPopup.height,
  };

  private dragging = false;
  private resizing = false;
  private resizeDirection: ResizeDirection | null = null;

  private startMouseX = 0;
  private startMouseY = 0;
  private startLeft = 0;
  private startTop = 0;
  private startWidth = 0;
  private startHeight = 0;

  private readonly minWidth = 420;
  private readonly minHeight = 300;

  showColorSelection = false;
  pendingCard: CardInstance | null = null;
  chosenColor: PlayColor | null = null;
  chosenColorCardId: string | null = null;
  showMinusOneSelection = false;
  selectedMinusOnePlayer: string | null = null;

  playerCardCounts: Record<string, number> = {};

  constructor(
    private router: Router,
    private cdr: ChangeDetectorRef,
    private api: ApiService,
    private ngZone: NgZone,
  ) {}

  ngOnInit() {
    const gameId = localStorage.getItem('gameId');

    if (!gameId) {
      console.error('No game ID found in localStorage');
      return;
    }

    this.api.getGameState(gameId).subscribe({
      next: (state) => {
        this.gameState = state;
        this.visibleCurrentPlayerId = state.currentPlayerId;
        this.visibleNextPlayerId = state.nextPlayerId;
        if (this.withAI && this.gameState.players.length > 1) {
          this.aiPlayerId = this.gameState.players[this.gameState.players.length - 1].playerId;
        }

        if (this.withAI) {
          this.showPassScreen = false;
          this.pendingNextPlayerIndex = null;
          this.isInitialPassScreen = false;
        } else {
          const currentPlayerIndex = state.players.findIndex(
            (p) => p.playerId === state.currentPlayerId,
          );

          const initialPassKey = `initialPassScreenShown_${state.gameId}`;
          const initialPassAlreadyShown = sessionStorage.getItem(initialPassKey) === 'true';

          const passScreenStateKey = `passScreenState_${state.gameId}`;
          const savedPassScreenState = JSON.parse(
            sessionStorage.getItem(passScreenStateKey) || 'null',
          ) as { playerId: string; info: string | null } | null;

          this.showPassScreen = true;
          this.pendingNextPlayerIndex = currentPlayerIndex === -1 ? 0 : currentPlayerIndex;

          if (savedPassScreenState?.playerId === state.currentPlayerId) {
            this.passScreenInfo = savedPassScreenState.info;
            this.isInitialPassScreen = false;
          } else {
            this.isInitialPassScreen = !initialPassAlreadyShown;
          }
        }

        const currentPlayerId = this.gameState.currentPlayerId;
        const token = this.getPlayerTokenById(currentPlayerId);

        if (!token) {
          console.error('No token found for:', currentPlayerId);
          return;
        }

        this.api.getHand(gameId, token).subscribe({
          next: (hand) => {
            this.hand = hand.cards;
            this.playerHands[currentPlayerId] = hand.cards;
            this.updatePlayerCardCounts();
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error loading hand:', err);
          },
        });
      },
      error: (err) => {
        console.error('Error loading game state:', err);
      },
    });
  }

  ngOnDestroy() {
    if (this.unoTimer) {
      clearTimeout(this.unoTimer);
    }

    if (this.handCardClickTimer) {
      clearTimeout(this.handCardClickTimer);
    }

    if (this.topCardClickTimer) {
      clearTimeout(this.topCardClickTimer);
    }

    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  }

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => {
      this.ngZone.run(() => {
        this.updateCardSpacing();
        this.cdr.detectChanges();
      });
    });

    if (this.playAreaRef?.nativeElement) {
      this.resizeObserver.observe(this.playAreaRef.nativeElement);
    }

    if (this.handCardsRef?.nativeElement) {
      this.resizeObserver.observe(this.handCardsRef.nativeElement);
    }

    requestAnimationFrame(() => {
      this.ngZone.run(() => {
        this.updateCardSpacing();
        this.cdr.detectChanges();
      });
    });
  }

  openRules(): void {
    const width = this.defaultPopup.width;
    const height = this.defaultPopup.height;

    this.popup = {
      left: (window.innerWidth - width) / 2,
      top: (window.innerHeight - height) / 2,
      width,
      height,
    };

    this.showRules = true;
  }

  closeRules(): void {
    this.showRules = false;
    this.dragging = false;
    this.resizing = false;
    this.resizeDirection = null;
  }
  openPlayers(): void {
    this.showPlayers = true;
  }

  closePlayers(): void {
    this.showPlayers = false;
  }

  startDrag(event: PointerEvent): void {
    event.preventDefault();

    this.dragging = true;
    this.startMouseX = event.clientX;
    this.startMouseY = event.clientY;
    this.startLeft = this.popup.left;
    this.startTop = this.popup.top;
  }

  startResize(event: PointerEvent, direction: ResizeDirection): void {
    event.preventDefault();
    event.stopPropagation();

    this.resizing = true;
    this.resizeDirection = direction;

    this.startMouseX = event.clientX;
    this.startMouseY = event.clientY;
    this.startLeft = this.popup.left;
    this.startTop = this.popup.top;
    this.startWidth = this.popup.width;
    this.startHeight = this.popup.height;
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(value, max));
  }

  @HostListener('document:pointermove', ['$event'])
  onPointerMove(event: PointerEvent): void {
    if (!this.dragging && !this.resizing) return;

    const dx = event.clientX - this.startMouseX;
    const dy = event.clientY - this.startMouseY;
    const windowWidth = window.innerWidth;
    const windowHeight = window.innerHeight;

    if (this.dragging) {
      this.popup.left = this.clamp(this.startLeft + dx, 0, windowWidth - this.popup.width);
      this.popup.top = this.clamp(this.startTop + dy, 0, windowHeight - this.popup.height);
      return;
    }

    if (!this.resizeDirection) return;

    let left = this.startLeft;
    let top = this.startTop;
    let width = this.startWidth;
    let height = this.startHeight;

    if (this.resizeDirection.includes('e')) {
      width = this.startWidth + dx;

      if (width < this.minWidth) {
        width = this.minWidth;
      }
    }

    if (this.resizeDirection.includes('s')) {
      height = this.startHeight + dy;

      if (height < this.minHeight) {
        height = this.minHeight;
      }
    }

    if (this.resizeDirection.includes('w')) {
      width = this.startWidth - dx;
      left = this.startLeft + dx;

      if (width < this.minWidth) {
        width = this.minWidth;
        left = this.startLeft + this.startWidth - this.minWidth;
      }
    }

    if (this.resizeDirection.includes('n')) {
      height = this.startHeight - dy;
      top = this.startTop + dy;

      if (height < this.minHeight) {
        height = this.minHeight;
        top = this.startTop + this.startHeight - this.minHeight;
      }
    }

    if (left < 0) {
      width = width + left;
      left = 0;
    }

    if (top < 0) {
      height = height + top;
      top = 0;
    }

    if (left + width > windowWidth) {
      width = windowWidth - left;
    }

    if (top + height > windowHeight) {
      height = windowHeight - top;
    }

    width = Math.max(width, this.minWidth);
    height = Math.max(height, this.minHeight);

    this.popup = {
      left,
      top,
      width,
      height,
    };
  }

  @HostListener('document:keydown.enter', ['$event'])
  onEnterPressed(event: Event): void {
    if (this.showExitConfirm || this.showRules) {
      return;
    }

    if (this.gameFinished) {
      event.preventDefault();
      this.goHome();
      return;
    }

    if (this.showPassScreen && this.pendingNextPlayerIndex !== null && !this.loading) {
      event.preventDefault();
      this.confirmNextPlayer();
    }
  }

  selectCard(card: CardInstance, event?: MouseEvent) {
    if (event && event.detail > 1) return;
    if (!this.isMyTurn() || this.loading) return;

    this.passScreenInfo = null;
    this.receivedCardPreview = null;

    this.selectedCard = this.selectedCard === card ? null : card;
  }

  openCardPreview(card: CardInstance, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.handCardClickTimer) {
      clearTimeout(this.handCardClickTimer);
      this.handCardClickTimer = null;
    }

    if (this.topCardClickTimer) {
      clearTimeout(this.topCardClickTimer);
      this.topCardClickTimer = null;
    }

    this.previewCard = card;
  }

  closeCardPreview(): void {
    this.previewCard = null;
  }

  onTopCardClick(event: MouseEvent): void {
    event.preventDefault();

    if (this.topCardClickTimer) {
      clearTimeout(this.topCardClickTimer);
    }

    this.topCardClickTimer = setTimeout(() => {
      this.playCard();
      this.cdr.detectChanges();
      this.topCardClickTimer = null;
    }, 120);
  }

  openResultInput() {
    this.error = null;
    this.resultInput = null;
    this.showResultInput = true;

    this.cdr.detectChanges();

    requestAnimationFrame(() => {
      this.resultInputFieldRef?.nativeElement.focus();
    });
  }

  onResultInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.resultInput = input.value === '' ? null : Number(input.value);
  }

  isResultInputValid(): boolean {
    return this.resultInput !== null && this.resultInput >= 0 && this.resultInput <= 9;
  }

  confirmResultInput() {
    if (!this.selectedCard || !this.isResultInputValid()) return;

    this.playSelectedCardWithBackend({
      answer: this.resultInput,
    });
  }

  cancelResultInput() {
    this.showResultInput = false;
    this.resultInput = null;
  }

  updatePlayerCardCounts() {
    this.playerCardCounts = {};

    for (const player of this.gameState.players) {
      this.playerCardCounts[player.playerId] = player.cardCount ?? 0;
    }

    this.scheduleCardSpacingUpdate();
  }

  get directionImage(): string {
    return this.gameState?.direction === 'clockwise'
      ? 'direction-arrows/direction-arrows-clockwise.png'
      : 'direction-arrows/direction-arrows-counterclockwise.png';
  }

  private scheduleCardSpacingUpdate(): void {
    requestAnimationFrame(() => {
      this.ngZone.run(() => {
        this.updateCardSpacing();
        this.cdr.detectChanges();
      });
    });
  }

  private calculateGap(
    availableSpace: number,
    cardSize: number,
    cardCount: number,
    normalGap: number,
    maxOverlapRatio: number,
  ): number {
    if (cardCount <= 1 || availableSpace <= 0 || cardSize <= 0) {
      return normalGap;
    }

    const naturalSize = cardCount * cardSize + (cardCount - 1) * normalGap;

    if (naturalSize <= availableSpace) {
      return normalGap;
    }

    const availableStep = (availableSpace - cardSize) / (cardCount - 1);
    const minimumStep = cardSize * (1 - maxOverlapRatio);

    return Math.max(availableStep, minimumStep) - cardSize;
  }

  private calculateSideMargin(
    availableHeight: number,
    cardLayoutHeight: number,
    rotatedCardSize: number,
    cardCount: number,
  ): number {
    const gap = this.calculateGap(availableHeight, rotatedCardSize, cardCount, 2, 0.65);

    return (rotatedCardSize + gap - cardLayoutHeight) / 2;
  }

  updateCardSpacing(): void {
    const playArea = this.playAreaRef?.nativeElement;
    const handCards = this.handCardsRef?.nativeElement;

    if (!playArea) return;

    const handCard = handCards?.querySelector('.hand-card') as HTMLElement | null;

    if (handCards && handCard) {
      this.handCardStep = this.calculateGap(
        handCards.clientWidth,
        handCard.offsetWidth,
        this.hand.length,
        12,
        0.85,
      );
    }

    const topCards = playArea.querySelector('.opponent-top .opponent-cards') as HTMLElement | null;
    const topCard = topCards?.querySelector('.opponent-card-back') as HTMLElement | null;

    if (topCards && topCard) {
      this.topOpponentCardStep = this.calculateGap(
        topCards.clientWidth,
        topCard.offsetWidth,
        topCards.querySelectorAll('.opponent-card-back').length,
        2,
        0.45,
      );
    }

    const availableSideHeight = playArea.clientHeight - 80;

    const leftCards = playArea.querySelector(
      '.opponent-left .opponent-cards',
    ) as HTMLElement | null;
    const leftCard = leftCards?.querySelector('.opponent-card-back') as HTMLElement | null;

    if (leftCards && leftCard) {
      this.leftOpponentCardMargin = this.calculateSideMargin(
        availableSideHeight,
        leftCard.offsetHeight,
        leftCard.offsetWidth,
        leftCards.querySelectorAll('.opponent-card-back').length,
      );
    }

    const rightCards = playArea.querySelector(
      '.opponent-right .opponent-cards',
    ) as HTMLElement | null;
    const rightCard = rightCards?.querySelector('.opponent-card-back') as HTMLElement | null;

    if (rightCards && rightCard) {
      this.rightOpponentCardMargin = this.calculateSideMargin(
        availableSideHeight,
        rightCard.offsetHeight,
        rightCard.offsetWidth,
        rightCards.querySelectorAll('.opponent-card-back').length,
      );
    }
  }

  getOtherPlayers(): string[] {
    if (!this.gameState?.players?.length) return [];

    return this.gameState.players
      .filter((p) => p.playerId !== this.gameState.currentPlayerId)
      .map((p) => p.playerId);
  }

  private getPlayersInSeatOrder(): PlayerState[] {
    return [...(this.gameState?.players ?? [])].sort((a, b) => a.playerPosition - b.playerPosition);
  }

  getRotatedOtherPlayers(): PlayerState[] {
    if (!this.gameState?.players?.length) return [];

    if (this.isSinglePlayerVsAi()) {
      return this.gameState.players.filter((player) => this.isAiPlayer(player.playerId));
    }

    const players = this.getPlayersInSeatOrder();
    const currentPlayerId = this.getVisibleCurrentPlayerId();
    const currentIndex = players.findIndex((p) => p.playerId === currentPlayerId);

    if (currentIndex === -1) {
      return players.filter((p) => p.playerId !== currentPlayerId);
    }

    return [...players.slice(currentIndex + 1), ...players.slice(0, currentIndex)];
  }

  getOpponentPositionClass(index: number): string {
    const opponentCount = this.getRotatedOtherPlayers().length;

    if (opponentCount === 1) {
      return 'opponent-top';
    }

    if (opponentCount === 2) {
      return index === 0 ? 'opponent-left' : 'opponent-right';
    }

    if (opponentCount === 3) {
      if (index === 0) return 'opponent-left';
      if (index === 1) return 'opponent-top';
      return 'opponent-right';
    }

    return 'opponent-top';
  }

  confirmNextPlayer() {
    if (this.pendingNextPlayerIndex === null && !this.withAI) return;

    if (this.isInitialPassScreen) {
      sessionStorage.setItem(`initialPassScreenShown_${this.gameState.gameId}`, 'true');
    }

    sessionStorage.removeItem(`passScreenState_${this.gameState.gameId}`);

    this.turnTransitionPending = false;
    this.pendingNextPlayerIndex = null;
    this.showPassScreen = false;
    this.isInitialPassScreen = false;
    this.selectedCard = null;
    this.showResultInput = false;
    this.showColorSelection = false;
    this.showMinusOneSelection = false;
    this.pendingCard = null;
    this.selectedMinusOnePlayer = null;
    this.error = null;
    this.skippedPlayerId = null;
    this.passScreenInfo = null;

    this.api.getGameState(this.gameState.gameId).subscribe({
      next: (state) => {
        this.gameState = state;
        this.visibleCurrentPlayerId = state.currentPlayerId;
        this.visibleNextPlayerId = state.nextPlayerId;

        const currentPlayerId = this.gameState.currentPlayerId;

        this.passScreenInfo = this.receivedCardMessages[currentPlayerId] ?? null;
        this.receivedCardPreview = this.receivedCardCards[currentPlayerId] ?? null;

        delete this.receivedCardMessages[currentPlayerId];
        delete this.receivedCardCards[currentPlayerId];

        this.cdr.detectChanges();

        this.currentPlayerIndex = this.gameState.players.findIndex(
          (p) => p.playerId === currentPlayerId,
        );

        if (this.isAiPlayer(currentPlayerId)) {
          this.showAiTurn = true;
          this.aiTurnMessage = 'Artificial Player is thinking...';

          this.loading = false;
          this.cdr.detectChanges();

          return;
        }

        this.showAiTurn = false;
        this.aiTurnMessage = '';

        const token = this.getPlayerTokenById(currentPlayerId);

        if (!token) {
          console.error('No token found for:', currentPlayerId);
          return;
        }

        this.api.getHand(this.gameState.gameId, token).subscribe({
          next: (hand) => {
            this.hand = hand.cards;
            this.playerHands[currentPlayerId] = hand.cards;
            this.updatePlayerCardCounts();

            this.loading = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error loading hand:', err);
            this.loading = false;
            this.cdr.detectChanges();
          },
        });
      },
    });
  }

  playCard() {
    if (!this.selectedCard || !this.isMyTurn() || this.loading) return;

    this.passScreenInfo = null;
    this.receivedCardPreview = null;

    const played = this.selectedCard;

    if (
      this.chosenColor &&
      this.gameState.topDiscardCard?.value === 'cloverleaf' &&
      played.color !== this.chosenColor &&
      played.color !== 'black'
    ) {
      this.error = `You must play a ${this.chosenColor} card or a special card.`;
      return;
    }

    if (played.color === 'black' && played.value === 'cloverleaf') {
      this.pendingCard = played;
      this.showColorSelection = true;
      return;
    }

    if (played.value === '-1') {
      this.pendingCard = played;
      this.selectedMinusOnePlayer = null;
      this.showMinusOneSelection = true;
      return;
    }

    if (
      played.value === '+2' ||
      played.value === '+3' ||
      played.value === 'reverse' ||
      played.value === 'hourglass'
    ) {
      this.playSelectedCardWithBackend();
      return;
    }

    this.openResultInput();
  }

  chooseColor(color: PlayColor) {
    if (!this.pendingCard) return;
    this.chosenColor = color;
    this.chosenColorCardId = this.pendingCard.cardId;

    this.selectedCard = this.pendingCard;

    this.playSelectedCardWithBackend({
      chosenColor: color,
    });

    this.pendingCard = null;
    this.showColorSelection = false;
  }

  chosenColorValue(): string | null {
    if (!this.chosenColor) return null;

    const map: Record<string, string> = {
      red: '#ff3347',
      blue: '#334bff',
      green: '#6cbf2a',
      yellow: '#f0c51b',
      black: '#222',
    };

    return map[this.chosenColor] ?? '#888';
  }

  cancelColorSelection() {
    this.pendingCard = null;
    this.showColorSelection = false;
  }

  selectMinusOnePlayer(player: string) {
    this.selectedMinusOnePlayer = player;
  }

  giveCardToSelectedPlayer(card: CardInstance) {
    if (!this.pendingCard || !this.selectedMinusOnePlayer) return;

    if (card.cardId === this.pendingCard.cardId) {
      this.error = 'You cannot give away the -1 card itself.';
      return;
    }

    this.selectedCard = this.pendingCard;

    const fromPlayerId = this.gameState.currentPlayerId;
    const targetPlayerId = this.selectedMinusOnePlayer;

    this.receivedCardMessages[targetPlayerId] =
      `You received the highlighted card from ${this.displayPlayerName(fromPlayerId)}.`;
    this.receivedCardCards[targetPlayerId] = card;

    this.playSelectedCardWithBackend({
      cardToGiveId: card.cardId,
      targetPlayerId: this.selectedMinusOnePlayer,
    });

    this.pendingCard = null;
    this.selectedMinusOnePlayer = null;
    this.showMinusOneSelection = false;
  }

  cancelMinusOneSelection() {
    this.pendingCard = null;
    this.selectedMinusOnePlayer = null;
    this.showMinusOneSelection = false;
  }
  startUnoTimer(playerToken: string): void {
    this.showUnoButton = true;
    this.unoClicked = false;

    if (this.unoTimer) {
      clearTimeout(this.unoTimer);
    }

    this.loading = false;
    this.cdr.detectChanges();

    this.unoTimer = setTimeout(() => {
      this.unoTimer = null;

      if (!this.unoClicked) {
        this.showUnoButton = false;
        this.drawPenaltyCards(playerToken);
        return;
      }

      this.finishUnoPhase();
    }, this.unoTimeLimitMs);
  }

  callUno(): void {
    this.unoClicked = true;
    this.showUnoButton = false;

    if (this.unoTimer) {
      clearTimeout(this.unoTimer);
      this.unoTimer = null;
    }

    this.finishUnoPhase();
  }

  private finishUnoPhase(): void {
    this.showUnoButton = false;
    this.unoClicked = false;
    this.loading = false;

    if (this.withAI && this.pendingStateAfterUno) {
      const nextState = this.pendingStateAfterUno;
      this.pendingStateAfterUno = null;

      this.showAiThinkingBeforeApplyingState(nextState);
      return;
    }

    this.pendingNextPlayerIndex = this.gameState.players.findIndex(
      (p) => p.playerId === this.gameState.currentPlayerId,
    );

    if (this.withAI) {
      this.confirmNextPlayer();
    } else {
      this.showPassScreen = true;
      this.cdr.detectChanges();
    }
  }

  drawPenaltyCards(playerToken: string): void {
    this.pendingStateAfterUno = null;
    const penalizedPlayerId = this.getVisibleCurrentPlayerId();
    this.loading = true;

    this.api.unoPenalty(this.gameState.gameId, playerToken).subscribe({
      next: (state) => {
        if (!this.withAI) {
          this.gameState = state;
        }
        this.api.getHand(this.gameState.gameId, playerToken).subscribe({
          next: (hand) => {
            this.hand = hand.cards;
            this.playerHands[penalizedPlayerId] = hand.cards;

            if (this.withAI) {
              this.playerCardCounts[penalizedPlayerId] = this.hand.length;
            } else {
              this.updatePlayerCardCounts();
            }

            this.error = 'You forgot to press UNO! Penalty cards were drawn.';
            this.loading = false;
            this.cdr.detectChanges();

            setTimeout(() => {
              this.error = null;

              if (this.withAI) {
                this.showAiThinkingBeforeApplyingState(state);
                return;
              }

              this.finishUnoPhase();
            }, 3500);
          },
          error: (err) => {
            console.error('Error loading hand after UNO penalty:', err);
            this.error = 'Hand could not be loaded after UNO penalty';
            this.loading = false;
            this.cdr.detectChanges();
          },
        });
      },
      error: (err) => {
        console.error('Error applying UNO penalty:', err);
        this.error = err.error?.errorMessage || 'UNO penalty failed';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
  private updateAiActionMessage(aiActions?: AiAction[]): void {
    const actions = aiActions ?? [];

    const playedCardsCount = actions.filter((action) => action.type === 'PLAY_CARD').length;
    const hasMultipleAiPlays = playedCardsCount > 1;

    const actionsToShow = actions.filter((action) => {
      const cardValue = action.card?.value;

      const isSpecialCard =
        cardValue === '+2' ||
        cardValue === '+3' ||
        cardValue === 'hourglass' ||
        cardValue === 'reverse' ||
        cardValue === 'cloverleaf' ||
        cardValue === '-1';

      return (
        action.type === 'DRAW_CARDS' ||
        action.type === 'CHOOSE_COLOR' ||
        action.type === 'GIVE_CARD' ||
        hasMultipleAiPlays ||
        (action.type === 'PLAY_CARD' && isSpecialCard)
      );
    });

    const message = actionsToShow
      .map((action) => this.createAiActionMessage(action))
      .filter((message): message is string => !!message)
      .join(' ');

    this.passScreenInfo = message || null;
  }

  private createAiActionMessage(action: AiAction): string | null {
    if (
      action.type === 'PLAY_CARD' &&
      action.card?.value !== '+2' &&
      action.card?.value !== '+3' &&
      action.card?.value !== 'hourglass' &&
      action.card?.value !== 'reverse' &&
      action.card?.value !== 'cloverleaf' &&
      action.card?.value !== '-1'
    ) {
      return '';
    }
    if (action.type === 'PLAY_CARD' && action.card?.value === 'reverse') {
      return 'Artificial Player played a Reverse card and played again.';
    }

    if (action.type === 'PLAY_CARD' && action.card?.value === '+2') {
      return 'Artificial Player played a +2 card and played again.';
    }

    if (action.type === 'PLAY_CARD' && action.card?.value === '+3') {
      return 'Artificial Player played a +3 card and played again..';
    }

    if (action.type === 'PLAY_CARD' && action.card?.value === 'hourglass') {
      return 'Artificial Player played an Hourglass card and played again.';
    }

    if (action.type === 'PLAY_CARD' && action.card?.value === 'cloverleaf') {
      return action.message ?? 'Artificial Player played a Cloverleaf card.';
    }

    if (action.type === 'PLAY_CARD' && action.card?.value === '-1') {
      return 'Artificial Player played a -1 card.';
    }

    return action.message ?? null;
  }

  drawCard() {
    if (!this.isMyTurn() || this.loading || this.turnTransitionPending) return;

    const token = this.getCurrentPlayerToken();

    if (!token) return;

    const drawingPlayerId = this.gameState.currentPlayerId;

    this.loading = true;
    this.turnTransitionPending = true;
    this.error = null;
    this.passScreenInfo = null;
    this.receivedCardPreview = null;

    this.api.drawCard(this.gameState.gameId, token).subscribe({
      next: (response) => {
        const state = response.gameState;
        const aiActions = response.aiActions;

        if (response.chosenCardColor) {
          this.chosenColor = response.chosenCardColor;
          this.chosenColorCardId = state.topDiscardCard?.cardId ?? null;
        }

        if (this.withAI) {
          this.api.getHand(this.gameState.gameId, token).subscribe({
            next: (hand) => {
              this.hand = hand.cards;
              this.playerHands[drawingPlayerId] = hand.cards;
              this.playerCardCounts[drawingPlayerId] = this.hand.length;

              this.scheduleCardSpacingUpdate();

              this.loading = false;
              this.cdr.detectChanges();

              this.showAiThinkingBeforeApplyingState(state, aiActions);
            },
            error: (err) => {
              console.error('Error loading hand after drawing:', err);
              this.loading = false;
              this.turnTransitionPending = false;
              this.cdr.detectChanges();
            },
          });

          return;
        }

        this.gameState = state;

        this.api.getHand(this.gameState.gameId, token).subscribe({
          next: (hand) => {
            this.hand = hand.cards;
            this.playerHands[drawingPlayerId] = hand.cards;

            this.updatePlayerCardCounts();

            setTimeout(() => {
              const nextIndex = this.gameState.players.findIndex(
                (p) => p.playerId === this.gameState.currentPlayerId,
              );

              this.pendingNextPlayerIndex = nextIndex;

              const nextPlayer = this.gameState.players[nextIndex];

              if (this.isAiPlayer(nextPlayer.playerId)) {
                this.showAiThinking();
              } else {
                if (this.withAI) {
                  this.showAiTurn = true;
                  this.aiTurnMessage = 'Artificial Player is thinking...';
                  this.loading = false;
                  this.cdr.detectChanges();

                  setTimeout(() => {
                    this.confirmNextPlayer();
                  }, this.turnPreviewDelayMs);
                } else {
                  this.showPassScreen = true;

                  sessionStorage.setItem(
                    `passScreenState_${this.gameState.gameId}`,
                    JSON.stringify({
                      playerId: this.gameState.currentPlayerId,
                      info: null,
                    }),
                  );

                  this.loading = false;
                  this.turnTransitionPending = false;
                  this.cdr.detectChanges();
                }
              }
            }, this.turnPreviewDelayMs);
          },
        });
      },
      error: (err) => {
        console.error('Error drawing card:', err);
        this.error = err.error?.errorMessage || 'Card could not be drawn';
        this.loading = false;
        this.turnTransitionPending = false;
      },
    });
  }

  isMyTurn(): boolean {
    return (
      !this.gameFinished &&
      !this.showPassScreen &&
      !this.turnTransitionPending &&
      !this.showAiTurn &&
      !this.isAiPlayer(this.gameState.currentPlayerId)
    );
  }

  private isSinglePlayerVsAi(): boolean {
    return this.withAI && this.gameState.players.length === 2;
  }

  private isAiPlayer(playerId: string): boolean {
    return this.aiPlayerId === playerId || this.displayPlayerName(playerId) === 'Artificial Player';
  }
  private showAiThinking(): void {
    this.showAiTurn = true;
    this.aiTurnMessage = 'Artificial Player is thinking...';

    this.loading = false;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.confirmNextPlayer();
    }, this.turnPreviewDelayMs);
  }

  private showAiThinkingBeforeApplyingState(nextState: GameState, aiActions?: AiAction[]): void {
    this.showAiTurn = true;
    this.aiTurnMessage = 'Artificial Player is thinking...';
    this.loading = false;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.gameState = nextState;
      this.visibleCurrentPlayerId = nextState.currentPlayerId;

      this.currentPlayerIndex = nextState.players.findIndex(
        (p) => p.playerId === nextState.currentPlayerId,
      );

      this.showAiTurn = false;
      this.aiTurnMessage = '';
      this.showPassScreen = false;

      this.handleAiActions(aiActions);

      this.updatePlayerCardCounts();
      const winner = nextState.players.find((player) => player.cardCount === 0);

      if (winner) {
        this.finishGame(winner.playerId);
        return;
      }

      const token = this.getPlayerTokenById(nextState.currentPlayerId);

      if (!token) {
        console.error('No token found for:', nextState.currentPlayerId);
        this.loading = false;
        this.cdr.detectChanges();
        return;
      }

      this.api.getHand(nextState.gameId, token).subscribe({
        next: (hand) => {
          this.hand = hand.cards;
          this.playerHands[nextState.currentPlayerId] = hand.cards;

          this.loading = false;
          this.turnTransitionPending = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error loading hand after AI turn:', err);
          this.loading = false;
          this.turnTransitionPending = false;
          this.cdr.detectChanges();
        },
      });
    }, this.turnPreviewDelayMs);
  }

  private isUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
  }

  get displayPlayers(): DisplayPlayer[] {
    return this.gameState.players.map((player, index) => ({
      id: player.playerId,
      name: this.displayPlayerName(player.playerId),
      position: index + 1,
      isRealId: this.isUuid(player.playerId),
    }));
  }

  displayPlayerName(playerId: string): string {
    const player = this.gameState.players.find((p) => p.playerId === playerId);

    if (!player) return playerId;

    if (player.isAi) {
      return 'Artificial Player';
    }

    const position = player.playerPosition;

    const savedName = this.playerNames[position]?.trim();
    return savedName || `Player ${position + 1}`;
  }

  goHome(): void {
    void this.router.navigate(['/start-game']);
  }

  openExitConfirm(): void {
    this.showExitConfirm = true;
  }

  closeExitConfirm(): void {
    this.showExitConfirm = false;
  }

  confirmExit(): void {
    this.showExitConfirm = false;
    this.goHome();
  }

  getCardBacks(playerId: string): number[] {
    const count = this.playerCardCounts[playerId] ?? 0;
    return Array(count);
  }

  cardImageUrl(card: CardInstance): string {
    return `${environment.apiUrl}/deck-images/${this.deckId}/${card.imageName}`;
  }
  private getPlayerIdWithOffsetFromPlayerId(playerId: string, offset: number): string | null {
    const players = this.getPlayersInSeatOrder();

    if (!players.length) {
      return null;
    }

    const startIndex = players.findIndex((player) => player.playerId === playerId);

    if (startIndex === -1) {
      return null;
    }

    const step = this.gameState.direction === 'clockwise' ? offset : -offset;
    const targetIndex = (startIndex + step + players.length) % players.length;

    return players[targetIndex]?.playerId ?? null;
  }

  private isTwoPlayerSelfTurnAgain(playedByPlayerId: string, nextPlayerId: string): boolean {
    return this.gameState.players.length === 2 && playedByPlayerId === nextPlayerId;
  }

  private createSelfTurnAgainInfo(playedCard: CardInstance): string | null {
    if (playedCard.value === 'hourglass') {
      return 'You can play again because you played an Hourglass card.';
    }

    if (playedCard.value === '+2') {
      return 'You can play again because you played a +2 card.';
    }

    if (playedCard.value === '+3') {
      return 'You can play again because you played a +3 card.';
    }

    if (playedCard.value === 'reverse') {
      return 'You can play again because you played a Reverse card.';
    }

    return null;
  }

  private createPassScreenInfoAfterCardPlay(
    playedCard: CardInstance,
    state: GameState,
    cardCountsBeforePlay: Record<string, number>,
    skippedPlayerBeforeId: string | null,
  ): string | null {
    const drawPenaltyCount = playedCard.value === '+2' ? 2 : playedCard.value === '+3' ? 3 : 0;

    if (drawPenaltyCount > 0) {
      const affectedPlayer = state.players.find((player) => {
        const oldCount = cardCountsBeforePlay[player.playerId] ?? 0;
        const newCount = player.cardCount ?? 0;

        return newCount - oldCount === drawPenaltyCount;
      });

      const affectedPlayerId = affectedPlayer?.playerId ?? skippedPlayerBeforeId;

      if (!affectedPlayerId) {
        return null;
      }

      const cardText = drawPenaltyCount === 2 ? 'two cards' : 'three cards';

      return `${this.displayPlayerName(affectedPlayerId)} had to draw ${cardText} and cannot play this turn.`;
    }

    if (playedCard.value === 'hourglass' && skippedPlayerBeforeId) {
      return `${this.displayPlayerName(skippedPlayerBeforeId)} was skipped.`;
    }

    if (playedCard.value === 'reverse' && state.players.length > 2) {
      return `The direction changed. ${this.displayPlayerName(state.currentPlayerId)} plays next.`;
    }

    return null;
  }

  private playSelectedCardWithBackend(request: any = {}) {
    const playedCard = this.selectedCard;

    if (!playedCard) return;

    const shouldClearChosenColorAfterPlay =
      this.chosenColor !== null && playedCard.cardId !== this.chosenColorCardId;

    const skippedPlayerBeforeId =
      playedCard.value === '+2' || playedCard.value === '+3' || playedCard.value === 'hourglass'
        ? this.getPlayerIdWithOffsetFromPlayerId(this.gameState.currentPlayerId, 1)
        : null;

    const cardCountsBeforePlay = Object.fromEntries(
      this.gameState.players.map((player) => [player.playerId, player.cardCount ?? 0]),
    );
    const token = this.getPlayerTokenById(this.gameState.currentPlayerId);

    if (!token) {
      console.error('No token found');
      return;
    }

    const playedByPlayerId = this.gameState.currentPlayerId;

    this.loading = true;
    this.turnTransitionPending = true;
    this.error = null;

    this.api.playCard(this.gameState.gameId, playedCard, request, token).subscribe({
      next: (response) => {
        const state = response.gameState;
        const aiActions = response.aiActions;

        if (shouldClearChosenColorAfterPlay) {
          this.chosenColor = null;
          this.chosenColorCardId = null;
        }

        if (response.chosenCardColor) {
          this.chosenColor = response.chosenCardColor;
          this.chosenColorCardId = state.topDiscardCard?.cardId ?? null;
        }

        if (this.withAI) {
          this.gameState = {
            ...this.gameState,
            topDiscardCard: playedCard,
          };

          this.selectedCard = null;
          this.showResultInput = false;
          this.resultInput = null;

          this.api.getHand(this.gameState.gameId, token).subscribe({
            next: (hand) => {
              this.hand = hand.cards;
              this.playerHands[playedByPlayerId] = hand.cards;
              this.playerCardCounts[playedByPlayerId] = this.hand.length;

              if (this.hand.length === 0) {
                this.finishGame(playedByPlayerId);
                return;
              }

              if (this.hand.length === 1) {
                this.pendingStateAfterUno = state;

                this.loading = false;
                this.turnTransitionPending = false;
                this.cdr.detectChanges();
                this.startUnoTimer(token);
                return;
              }

              this.loading = false;
              this.cdr.detectChanges();

              setTimeout(() => {
                const skipAiThinking =
                  playedCard.value === '+2' ||
                  playedCard.value === '+3' ||
                  playedCard.value === 'hourglass' ||
                  playedCard.value === 'reverse';

                if (skipAiThinking) {
                  this.gameState = state;
                  this.visibleCurrentPlayerId = state.currentPlayerId;
                  this.visibleNextPlayerId = state.nextPlayerId;

                  this.currentPlayerIndex = state.players.findIndex(
                    (p) => p.playerId === state.currentPlayerId,
                  );

                  if (this.isTwoPlayerSelfTurnAgain(playedByPlayerId, state.currentPlayerId)) {
                    this.passScreenInfo = this.createSelfTurnAgainInfo(playedCard);
                  }

                  this.showAiTurn = false;
                  this.aiTurnMessage = '';
                  this.showPassScreen = false;

                  this.updatePlayerCardCounts();

                  const nextToken = this.getPlayerTokenById(state.currentPlayerId);

                  if (!nextToken) {
                    console.error('No token found for:', state.currentPlayerId);
                    this.loading = false;
                    this.cdr.detectChanges();
                    return;
                  }

                  this.api.getHand(state.gameId, nextToken).subscribe({
                    next: (nextHand) => {
                      this.hand = nextHand.cards;
                      this.playerHands[state.currentPlayerId] = nextHand.cards;

                      this.loading = false;
                      this.turnTransitionPending = false;
                      this.cdr.detectChanges();
                    },
                    error: (err) => {
                      console.error('Error loading hand after skipped AI turn:', err);
                      this.loading = false;
                      this.turnTransitionPending = false;
                      this.cdr.detectChanges();
                    },
                  });

                  return;
                }
                this.showAiThinkingBeforeApplyingState(state, aiActions);
              }, this.turnPreviewDelayMs);
            },
            error: (err) => {
              console.error('Error loading hand after playing card:', err);
              this.loading = false;
              this.turnTransitionPending = false;
              this.cdr.detectChanges();
            },
          });

          return;
        }
        this.gameState = state;

        this.selectedCard = null;
        this.showResultInput = false;
        this.resultInput = null;

        this.updatePlayerCardCounts();

        const passInfo = this.createPassScreenInfoAfterCardPlay(
          playedCard,
          state,
          cardCountsBeforePlay,
          skippedPlayerBeforeId,
        );

        const passTargetPlayerId = state.currentPlayerId;

        const backendNextPlayer = state.players.find((p) => p.playerId === passTargetPlayerId);

        if (backendNextPlayer?.isAi) {
          this.showAiTurn = true;
          this.aiTurnMessage = 'Artificial Player is thinking...';
          this.loading = false;
          this.cdr.detectChanges();

          setTimeout(() => {
            this.showAiTurn = false;
            this.aiTurnMessage = '';
            this.confirmNextPlayer();
          }, this.turnPreviewDelayMs);

          return;
        }

        this.api.getHand(this.gameState.gameId, token).subscribe({
          next: (hand) => {
            this.hand = hand.cards;
            this.playerHands[playedByPlayerId] = hand.cards;

            if (this.hand.length === 0) {
              this.updatePlayerCardCounts();
              this.finishGame(playedByPlayerId);
              return;
            }

            if (this.hand.length === 1) {
              this.turnTransitionPending = false;
              this.startUnoTimer(token);
              return;
            }

            this.pendingNextPlayerIndex = this.gameState.players.findIndex(
              (p) => p.playerId === passTargetPlayerId,
            );

            this.loading = false;
            this.cdr.detectChanges();

            setTimeout(() => {
              if (this.withAI) {
                this.confirmNextPlayer();
                return;
              }

              const samePlayerAgain = this.isTwoPlayerSelfTurnAgain(
                playedByPlayerId,
                passTargetPlayerId,
              );

              if (samePlayerAgain) {
                this.passScreenInfo = this.createSelfTurnAgainInfo(playedCard);
                this.visibleCurrentPlayerId = passTargetPlayerId;
                this.visibleNextPlayerId = this.gameState.nextPlayerId;

                this.loading = false;
                this.turnTransitionPending = false;
                this.cdr.detectChanges();

                return;
              }

              this.passScreenInfo = passInfo;
              this.showPassScreen = true;

              sessionStorage.setItem(
                `passScreenState_${this.gameState.gameId}`,
                JSON.stringify({
                  playerId: passTargetPlayerId,
                  info: passInfo,
                }),
              );

              this.loading = false;
              this.turnTransitionPending = false;
              this.cdr.detectChanges();
            }, this.turnPreviewDelayMs);
          },
        });
      },
      error: (err) => {
        console.error('Error playing card:', err);

        this.error =
          err.error?.errorMessage || err.headers?.get('X-Error-Message') || 'Card cannot be played';

        this.loading = false;
        this.turnTransitionPending = false;
        this.cdr.detectChanges();
      },
    });
  }

  getVisibleCurrentPlayerId(): string {
    return this.visibleCurrentPlayerId ?? this.gameState.currentPlayerId;
  }
  getVisibleNextPlayerId(): string {
    return this.visibleNextPlayerId ?? this.gameState.nextPlayerId;
  }

  getCardHighlightColor(card: CardInstance): string {
    const colors: Record<string, string> = {
      red: '#ff3347',
      blue: '#334bff',
      green: '#6cbf2a',
      yellow: '#f0c51b',
      black: '#ffffff',
    };

    return colors[card.color] ?? '#ffffff';
  }
  private handleAiActions(aiActions?: AiAction[]): void {
    const actions = aiActions ?? [];

    for (const action of actions) {
      if (action.type === 'GIVE_CARD' && action.targetPlayerId && action.card) {
        this.receivedCardMessages[action.targetPlayerId] =
          'You received the highlighted card from Artificial Player.';

        this.receivedCardCards[action.targetPlayerId] = action.card;

        if (action.targetPlayerId === this.gameState.currentPlayerId) {
          this.passScreenInfo = 'You received the highlighted card from Artificial Player.';
          this.receivedCardPreview = action.card;
        }
      }
    }

    this.updateAiActionMessage(actions);
  }
}
