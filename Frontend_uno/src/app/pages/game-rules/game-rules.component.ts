import { Component } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-game-rules',
  standalone: true,
  imports: [CommonModule, NgOptimizedImage],
  templateUrl: './game-rules.component.html',
  styleUrl: './game-rules.component.scss',
})
export class GameRulesComponent {}

