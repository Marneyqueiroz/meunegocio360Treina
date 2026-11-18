import { Button, Container, Spinner } from "react-bootstrap";

export default function SessionStatus({ error = false, onRetry }) {
  return (
    <main className="app-shell">
      <Container className="login-shell">
        <section className="text-center" aria-labelledby="session-status-title">
          {error ? (
            <>
              <h1 id="session-status-title" className="h2 mb-3">
                Não foi possível restaurar sua sessão
              </h1>
              <p className="text-secondary mb-4">
                Verifique sua conexão e tente novamente.
              </p>
              <Button type="button" variant="primary" onClick={onRetry}>
                Tentar novamente
              </Button>
            </>
          ) : (
            <div role="status" aria-live="polite" className="text-secondary">
              <Spinner animation="border" role="status" className="mb-3">
                <span className="visually-hidden">Carregando</span>
              </Spinner>
              <p>Verificando sua sessão…</p>
            </div>
          )}
        </section>
      </Container>
    </main>
  );
}
