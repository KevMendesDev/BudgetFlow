package br.com.budgetflow.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return validarDigitos(digits);
    }

    private boolean validarDigitos(String cpf) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (cpf.charAt(i) - '0') * (10 - i);
        }
        int remainder = sum % 11;
        int firstDigit = (remainder < 2) ? 0 : (11 - remainder);
        if (firstDigit != (cpf.charAt(9) - '0')) {
            return false;
        }
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (cpf.charAt(i) - '0') * (11 - i);
        }
        remainder = sum % 11;
        int secondDigit = (remainder < 2) ? 0 : (11 - remainder);
        return secondDigit == (cpf.charAt(10) - '0');
    }
}
