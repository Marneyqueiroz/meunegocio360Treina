import { Button, Container } from "react-bootstrap";
import { Link } from "react-router";
import { useEffect } from "react";

export default function NotFoundPage() {
  useEffect(() => {
    document.title = "Página não encontrada | MeuNegocio360";
  }, []);

  return (
    <main className="app-shell">
      <Container className="login-shell">
        <section className="text-center" aria-labelledby="not-found-title">
          <h1 id="not-found-title" className="h2 mb-3">
            Página não encontrada
          </h1>
          <p className="text-secondary mb-4">
            Confira o endereço ou volte ao início.
          </p>
          <Button as={Link} to="/" variant="primary">
            Ir para o início
          </Button>
        </section>
      </Container>
    </main>
  );
}
