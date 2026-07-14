export interface CurrentUser {
  id: number;
  nome: string;
  email: string;
  roles: string[];
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  telefone: string | null;
  senha: string;
}

export interface MessageResponse {
  message: string;
}

export interface ApiValidationError {
  errors?: Record<string, string>;
  error?: string;
  code?: string;
}
