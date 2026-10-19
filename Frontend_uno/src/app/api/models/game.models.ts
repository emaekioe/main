export interface CreatedPlayer {
  playerId: string;
  playerPosition: number;
  isAi: boolean;
  playerToken: string;
}

export interface GameCreatedResponse {
  gameId: string;
  players: CreatedPlayer[];
  gameState: GameState;
}

export interface PlayerHand {
  gameId: string;
  playerId: string;
  cards: CardInstance[];
  cardCount: number;
}

export interface PlayerState {
  playerId: string;
  cardCount: number;
  isAi: boolean;
  playerPosition: number;
}

export interface CardInstance {
  cardId: string;
  color: 'red' | 'blue' | 'green' | 'yellow' | 'black';
  value:
    | '0'
    | '1'
    | '2'
    | '3'
    | '4'
    | '5'
    | '6'
    | '7'
    | '8'
    | '9'
    | '+2'
    | '+3'
    | 'hourglass'
    | 'cloverleaf'
    | '-1'
    | 'reverse';
    variant: number;
    imageName: string;
}

export interface CardDeck {
  deckId: string;
  deckName: string;
}

export interface GameState {
  gameId: string;
  players: PlayerState[];
  currentPlayerId: string;
  nextPlayerId: string;
  direction: 'clockwise' | 'counterclockwise';
  topDiscardCard: CardInstance;
}

export interface DeckUploadResponse {
  deckId: string;
  deckName: string;
}

export type PlayColor = 'red' | 'blue' | 'green' | 'yellow';

export interface PlayCardRequest {
  cardId: string;
  answer?: number;
  chosenColor?: PlayColor;
  cardToGiveId?: string;
  targetPlayerId?: string;
}

export interface AiAction {
  type: 'PLAY_CARD' | 'DRAW_CARD' | 'DRAW_CARDS' | 'CHOOSE_COLOR' | 'GIVE_CARD';
  playerId: string;
  targetPlayerId?: string | null;
  card?: CardInstance | null;
  amount?: number | null;
  chosenColor?: PlayColor | null;
  message?: string | null;
}

export interface PlayCardResponse {
  gameState: GameState;
  chosenCardColor?: PlayColor | null;
  aiActions?: AiAction[];
}

