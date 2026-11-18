import { z } from "zod";

export const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Informe seu e-mail.")
    .max(160, "Informe um e-mail válido.")
    .email("Informe um e-mail válido."),
  senha: z
    .string()
    .min(1, "Informe sua senha.")
    .refine((value) => value.length >= 8 && value.length <= 72, {
      message: "A senha deve ter entre 8 e 72 caracteres.",
    }),
});
