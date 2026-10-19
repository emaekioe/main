import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-configuration',
  standalone: true,
  templateUrl: './configuration.component.html',
  styleUrl: './configuration.component.scss',
  imports: [CommonModule, MatSlideToggleModule, MatButtonModule],
})
export class ConfigurationComponent {
  withAI = localStorage.getItem('withAI') === 'true';
  playerCount = Number(localStorage.getItem('playerCount') || 2);
  startingCards = Number(localStorage.getItem('startingCards') || 7);
  saved = false;
  nameError: string | null = null;

  playerNames: string[] = JSON.parse(
    localStorage.getItem('playerNames') || '["Player 1", "Player 2", "Player 3", "Player 4"]',
  );

  hasUnsavedChanges = false;
  showLeaveWarning = false;
  onLeaveDecision: ((value: boolean) => void) | null = null;

  private savedSettings = {
    withAI: this.withAI,
    playerCount: this.playerCount,
    startingCards: this.startingCards,
    playerNames: [...this.playerNames],
  };

  constructor() {
    this.ensurePlayerNames();

    this.savedSettings = {
      withAI: this.withAI,
      playerCount: this.playerCount,
      startingCards: this.startingCards,
      playerNames: [...this.playerNames],
    };
  }

  setPlayerCount(n: number) {
    this.playerCount = n;

    if (this.playerCount !== 2) {
      this.withAI = false;
    }

    this.ensurePlayerNames();
    this.updateNameError();
    this.markUnsaved();
  }

  setStartingCards(n: number) {
    this.startingCards = n;
    this.markUnsaved();
  }

  toggleAI(val: boolean) {
    if (this.playerCount !== 2) {
      this.withAI = false;
      this.ensurePlayerNames();
      this.updateNameError();
      this.markUnsaved();
      return;
    }

    this.withAI = val;
    this.ensurePlayerNames();
    this.updateNameError();
    this.markUnsaved();
  }

  setPlayerName(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    this.playerNames[index] = input.value;

    this.updateNameError();
    this.markUnsaved();
  }

  private markUnsaved(): void {
    this.hasUnsavedChanges = !this.isSameAsSavedSettings();
    this.saved = false;
  }

  private isSameAsSavedSettings(): boolean {
    return (
      this.withAI === this.savedSettings.withAI &&
      this.playerCount === this.savedSettings.playerCount &&
      this.startingCards === this.savedSettings.startingCards &&
      JSON.stringify(this.playerNames) === JSON.stringify(this.savedSettings.playerNames)
    );
  }

  cancelNavigation(): void {
    this.showLeaveWarning = false;

    this.onLeaveDecision?.(false);
    this.onLeaveDecision = null;
  }

  leaveWithoutSaving(): void {
    this.showLeaveWarning = false;
    this.hasUnsavedChanges = false;

    this.onLeaveDecision?.(true);
    this.onLeaveDecision = null;
  }

  saveAndLeave(): void {
    this.updateNameError();

    if (this.hasInvalidPlayerNames()) {
      return;
    }

    this.saveChanges();

    this.showLeaveWarning = false;

    this.onLeaveDecision?.(true);
    this.onLeaveDecision = null;
  }

  ensurePlayerNames() {
    while (this.playerNames.length < this.playerCount) {
      this.playerNames.push(`Player ${this.playerNames.length + 1}`);
    }

    this.playerNames = this.playerNames.slice(0, this.playerCount);

    this.playerNames = this.playerNames.map((name, index) =>
      name === 'Artificial Player' ? `Player ${index + 1}` : name,
    );

    const lastIndex = this.playerCount - 1;

    if (this.withAI && this.playerCount === 2) {
      this.playerNames[lastIndex] = 'Artificial Player';
    }
  }

  normalizePlayerName(index: number): void {
    const value = this.playerNames[index]?.trim();

    if (!value) {
      this.playerNames[index] = `Player ${index + 1}`;
      this.updateNameError();
      this.markUnsaved();
      return;
    }

    this.playerNames[index] = value;
    this.updateNameError();
    this.markUnsaved();
  }

  isReservedAiName(index: number): boolean {
    const name = this.playerNames[index]?.trim().toLowerCase();

    return this.withAI && index !== this.playerCount - 1 && name === 'artificial player';
  }

  isDuplicateName(index: number): boolean {
    const currentName = this.playerNames[index]?.trim().toLowerCase();

    if (!currentName) {
      return false;
    }

    return this.playerNames
      .slice(0, this.playerCount)
      .some(
        (name, otherIndex) => otherIndex !== index && name.trim().toLowerCase() === currentName,
      );
  }

  hasInvalidPlayerNames(): boolean {
    return this.playerNames
      .slice(0, this.playerCount)
      .some((_, index) => this.isDuplicateName(index) || this.isReservedAiName(index));
  }

  private updateNameError(): void {
    const hasReservedAiName = this.playerNames
      .slice(0, this.playerCount)
      .some((_, index) => this.isReservedAiName(index));

    if (hasReservedAiName) {
      this.nameError = 'The name "Artificial Player" is reserved for the AI player.';
      return;
    }

    const hasDuplicateName = this.playerNames
      .slice(0, this.playerCount)
      .some((_, index) => this.isDuplicateName(index));

    this.nameError = hasDuplicateName ? 'Player names must be unique.' : null;
  }

  saveChanges() {
    if (!this.hasUnsavedChanges) {
      return;
    }

    this.playerNames = this.playerNames.map((name, index) => {
      const trimmedName = name.trim();
      return trimmedName || `Player ${index + 1}`;
    });

    this.ensurePlayerNames();
    this.updateNameError();

    if (this.hasInvalidPlayerNames()) {
      return;
    }

    this.nameError = null;

    localStorage.setItem('withAI', String(this.withAI));
    localStorage.setItem('playerCount', String(this.playerCount));
    localStorage.setItem('startingCards', String(this.startingCards));
    localStorage.setItem('playerNames', JSON.stringify(this.playerNames));

    this.savedSettings = {
      withAI: this.withAI,
      playerCount: this.playerCount,
      startingCards: this.startingCards,
      playerNames: [...this.playerNames],
    };

    this.hasUnsavedChanges = false;
    this.saved = true;
    setTimeout(() => (this.saved = false), 2000);
  }
}
