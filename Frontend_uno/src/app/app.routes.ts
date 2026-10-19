import { Routes } from '@angular/router';
import { SidebarLayoutComponent } from './layout/sidebar-layout/sidebar-layout.component';
import { StartGameComponent } from './pages/start-game/start-game.component';
import { GameRulesComponent } from './pages/game-rules/game-rules.component';
import { ConfigurationComponent } from './pages/configuration/configuration.component';
import { ActiveGameComponent } from './pages/active-game/active-game.component';
import { unsavedChangesGuard } from './guards/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: '',
    component: SidebarLayoutComponent,
    children: [
      { path: '', redirectTo: 'start-game', pathMatch: 'full' },
      { path: 'start-game', component: StartGameComponent },
      { path: 'game-rules', component: GameRulesComponent },
      { path: 'configuration', component: ConfigurationComponent, canDeactivate: [unsavedChangesGuard], },
    ],
  },
  { path: 'active-game', component: ActiveGameComponent },
];
