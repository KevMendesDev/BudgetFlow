import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		redirectTo: 'dashboard',
	},
	{
		path: '',
		loadComponent: () =>
			import('./layout/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent),
		children: [
			{
				path: 'login',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/login-page/login-page.component').then((m) => m.LoginPageComponent),
			},
			{
				path: 'cadastro',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/register-page/register-page.component').then((m) => m.RegisterPageComponent),
			},
			{
				path: 'verificar-email',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/verify-email-page/verify-email-page.component').then(
						(m) => m.VerifyEmailPageComponent
					),
			},
			{
				path: 'confirmar-email',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/confirm-email-page/confirm-email-page.component').then(
						(m) => m.ConfirmEmailPageComponent
					),
			},
			{
				path: 'esqueci-senha',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/forgot-password-page/forgot-password-page.component').then(
						(m) => m.ForgotPasswordPageComponent
					),
			},
			{
				path: 'redefinir-senha',
				canActivate: [guestGuard],
				loadComponent: () =>
					import('./features/auth/pages/reset-password-page/reset-password-page.component').then(
						(m) => m.ResetPasswordPageComponent
					),
			},
		],
	},
	{
		canActivate: [authGuard],
		path: '',
		loadComponent: () =>
			import('./layout/private-layout/private-layout.component').then((m) => m.PrivateLayoutComponent),
		children: [
			{
				path: 'dashboard',
				loadComponent: () =>
					import('./features/dashboard/pages/dashboard-page/dashboard-page.component').then(
						(m) => m.DashboardPageComponent
					),
			},
			{
				path: 'categorias',
				loadComponent: () =>
					import('./features/categorias/pages/categorias-page/categorias-page.component').then(
						(m) => m.CategoriasPageComponent
					),
			},
			{
				path: 'transacoes-recorrentes',
				loadComponent: () =>
					import(
						'./features/movimentacoes/pages/transacoes-recorrentes-page/transacoes-recorrentes-page.component'
					).then((m) => m.TransacoesRecorrentesPageComponent),
			},
			{
				path: 'planejamentos',
				loadComponent: () =>
					import('./features/planejamentos/pages/planejamentos-page/planejamentos-page.component').then(
						(m) => m.PlanejamentosPageComponent
					),
			},
			{
				path: 'periodos-financeiros',
				loadComponent: () =>
					import('./features/periodos/pages/periodos-financeiros-page/periodos-financeiros-page.component').then(
						(m) => m.PeriodosFinanceirosPageComponent
					),
			},
		],
	},
	{
		path: '**',
		redirectTo: 'dashboard',
	},
];
