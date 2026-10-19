import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs'
import { environment } from '../../../environments/environment';

import {
  CardInstance,
  CardDeck,
  GameCreatedResponse,
  GameState,
  PlayerHand,
  DeckUploadResponse,
  PlayCardRequest,
  PlayCardResponse,
} from '../models/game.models';


@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getCardDecks(): Observable<CardDeck[]> {
    return this.http.get<CardDeck[]>(`${this.baseUrl}/decks`);
  }

  createGame(
    deckId: string,
    playerCount: number,
    startingCards: number,
    withAI: boolean,
  ): Observable<GameCreatedResponse> {
    return this.http.post<GameCreatedResponse>(`${this.baseUrl}/games`, {
      deckId,
      playerCount,
      startingCards,
      withAI,
    });
  }

  getGameState(gameId: string): Observable<GameState> {
    return this.http.get<GameState>(`${this.baseUrl}/games/${gameId}`);
  }

  getHand(gameId: string, playerToken: string): Observable<PlayerHand> {
    const headers = new HttpHeaders({ 'X-Player-Token': playerToken });
    return this.http.get<PlayerHand>(`${this.baseUrl}/games/${gameId}/hand`, { headers });
  }

  drawCard(gameId: string, playerToken: string): Observable<PlayCardResponse> {
    const headers = new HttpHeaders({ 'X-Player-Token': playerToken });
    return this.http.post<PlayCardResponse>(
      `${this.baseUrl}/games/${gameId}/drawCard`,
      {},
      { headers },
    );
  }

  unoPenalty(gameId: string, playerToken: string): Observable<GameState> {
    const headers = new HttpHeaders({ 'X-Player-Token': playerToken });

    return this.http.post<GameState>(`${this.baseUrl}/games/${gameId}/unoPenalty`, {}, { headers });
  }

  playCard(
    gameId: string,
    card: CardInstance,
    request: Omit<PlayCardRequest, 'cardId'>,
    playerToken: string,
  ): Observable<PlayCardResponse> {
    const body: PlayCardRequest = {
      cardId: card.cardId,
      ...request,
    };
    const headers = new HttpHeaders({ 'X-Player-Token': playerToken });
    return this.http.post<PlayCardResponse>(`${this.baseUrl}/games/${gameId}/playCard`, body, {
      headers,
    });
  }

  uploadDeck(deckJsonFile: File, imageFiles: File[]): Observable<DeckUploadResponse> {
    const formData = new FormData();

    formData.append('deck', deckJsonFile);

    imageFiles.forEach((file) => {
      formData.append('images', file);
    });

    return this.http.post<DeckUploadResponse>(`${this.baseUrl}/decks`, formData);
  }
}
