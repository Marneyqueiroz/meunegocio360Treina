import { useEffect, useRef, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Alert, Button, Card, Container, Form, Spinner } from "react-bootstrap";
import { Navigate, useLocation, useNavigate } from "react-router";
import { ApiError } from "../../../shared/lib/http.js";
import { useAuth } from "../context/useAuth.js";
import { loginSchema } from "../schemas.js";

const genericNetworkMessage =
  "Não foi possível entrar agora. Verifique sua conexão e tente novamente.";
const currentTimeMs = () => Date.now();

function destinationFrom(value) {
  if (typeof value === "string") {
    return value.startsWith("/") && !value.startsWith("//") ? value : "/";
  }
  if (!value || typeof value !== "object") return "/";
  const pathname = value.pathname;
  if (
    typeof pathname !== "string" ||
    !pathname.startsWith("/") ||
    pathname.startsWith("//")
  )
    return "/";
  return `${pathname}${value.search || ""}${value.hash || ""}`;
}

export default function LoginPage() {
  const { status, login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const alertRef = useRef(null);
  const [serverAlert, setServerAlert] = useState("");
  const [focusServerAlert, setFocusServerAlert] = useState(false);
  const [lockUntil, setLockUntil] = useState(null);
  const [lockSeconds, setLockSeconds] = useState(0);
  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
    mode: "onSubmit",
    reValidateMode: "onChange",
    shouldFocusError: true,
    defaultValues: { email: "", senha: "" },
  });

  useEffect(() => {
    document.title = "Entrar | MeuNegocio360";
  }, []);

  useEffect(() => {
    if (focusServerAlert) alertRef.current?.focus();
  }, [focusServerAlert, serverAlert]);

  useEffect(() => {
    if (!lockUntil) return undefined;
    const updateRemaining = () => {
      const remaining = Math.max(
        0,
        Math.ceil((lockUntil - currentTimeMs()) / 1000),
      );
      setLockSeconds(remaining);
      if (remaining === 0) setLockUntil(null);
    };
    updateRemaining();
    const timer = window.setInterval(updateRemaining, 1000);
    return () => window.clearInterval(timer);
  }, [lockUntil]);

  if (status === "authenticated") {
    return <Navigate to={destinationFrom(location.state?.from)} replace />;
  }

  const onSubmit = async (values) => {
    setServerAlert("");
    setFocusServerAlert(false);
    try {
      await login(values);
      reset();
      navigate(destinationFrom(location.state?.from), { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.code === "VALIDATION_ERROR") {
        let firstField = null;
        for (const item of error.errors) {
          if (item.field === "email" || item.field === "senha") {
            firstField ||= item.field;
            setError(item.field, {
              type: "server",
              message: "Confira este campo.",
            });
          }
        }
        if (firstField) setFocus(firstField);
        else {
          setServerAlert("Confira os dados informados e tente novamente.");
          setFocusServerAlert(true);
        }
        return;
      }

      if (error instanceof ApiError && error.code === "INVALID_CREDENTIALS") {
        setServerAlert("E-mail ou senha inválidos.");
        setValue("senha", "");
        setFocus("senha");
        return;
      }

      if (
        error instanceof ApiError &&
        error.code === "ACCOUNT_TEMPORARILY_LOCKED"
      ) {
        const seconds = Math.max(1, error.retryAfter || 60);
        setLockUntil(currentTimeMs() + seconds * 1000);
        setLockSeconds(seconds);
        setServerAlert(
          `Muitas tentativas. Tente novamente em ${Math.ceil(seconds / 60)} ${seconds > 60 ? "minutos" : "minuto"}.`,
        );
        setFocusServerAlert(true);
        return;
      }

      setServerAlert(genericNetworkMessage);
      setFocusServerAlert(true);
    }
  };

  const emailField = register("email");
  const senhaField = register("senha");
  const emailErrorId = errors.email ? "email-error" : undefined;
  const senhaErrorId = errors.senha ? "senha-error" : undefined;
  const expired = location.state?.sessionExpired === true;
  const locked = lockSeconds > 0;

  return (
    <main className="app-shell">
      <Container className="login-shell">
        <Card className="login-card shadow-sm">
          <Card.Body>
            <div className="login-brand">
              <img
                className="auth-brand-logo"
                src="/logo_meunegocio360_v2_sem_fundo.png"
                alt="MeuNegocio360"
                width="448"
                height="134"
              />
            </div>

            <header className="text-center mb-4">
              <h1 className="h2 mb-2">Entrar</h1>
              <p className="text-secondary mb-0">
                Acesse sua conta para continuar.
              </p>
            </header>

            {expired && (
              <Alert variant="warning" role="alert" className="mb-4">
                Sua sessão expirou. Entre novamente.
              </Alert>
            )}

            {serverAlert && (
              <Alert
                ref={alertRef}
                variant="danger"
                role="alert"
                tabIndex={-1}
                className="mb-4"
              >
                {serverAlert}
              </Alert>
            )}

            <Form noValidate onSubmit={handleSubmit(onSubmit)}>
              <Form.Group className="mb-3" controlId="login-email">
                <Form.Label>E-mail</Form.Label>
                <Form.Control
                  {...emailField}
                  type="email"
                  autoComplete="username"
                  inputMode="email"
                  autoCapitalize="none"
                  autoCorrect="off"
                  maxLength={160}
                  placeholder="nome@empresa.com.br"
                  isInvalid={Boolean(errors.email)}
                  aria-invalid={Boolean(errors.email)}
                  aria-describedby={emailErrorId}
                />
                {errors.email && (
                  <Form.Control.Feedback id="email-error" type="invalid">
                    {errors.email.message}
                  </Form.Control.Feedback>
                )}
              </Form.Group>

              <Form.Group className="mb-4" controlId="login-senha">
                <Form.Label>Senha</Form.Label>
                <Form.Control
                  {...senhaField}
                  type="password"
                  autoComplete="current-password"
                  maxLength={72}
                  placeholder="Digite sua senha"
                  isInvalid={Boolean(errors.senha)}
                  aria-invalid={Boolean(errors.senha)}
                  aria-describedby={senhaErrorId}
                />
                {errors.senha && (
                  <Form.Control.Feedback id="senha-error" type="invalid">
                    {errors.senha.message}
                  </Form.Control.Feedback>
                )}
              </Form.Group>

              <Button
                type="submit"
                variant="primary"
                className="w-100"
                disabled={isSubmitting || locked}
                aria-busy={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <Spinner
                      size="sm"
                      animation="border"
                      aria-hidden="true"
                      className="me-2"
                    />
                    Entrando…
                  </>
                ) : locked ? (
                  `Aguarde ${Math.ceil(lockSeconds / 60)} min`
                ) : (
                  "Entrar"
                )}
              </Button>
            </Form>
          </Card.Body>
        </Card>
      </Container>
    </main>
  );
}
