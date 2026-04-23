export interface CurrentUser {
  id: number;
  nome: string;
  email: string;
  cpf: string;
  roles: string[];
}

export interface LoginRequest {
  cpf: string;
  senha: string;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  cpf: string;
  telefone: string | null;
  senha: string;
}

export interface ApiValidationError {
  errors?: Record<string, string>;
  error?: string;
}
