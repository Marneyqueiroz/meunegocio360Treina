import { useEffect } from "react";
import { Button, Card, Container, Navbar } from "react-bootstrap";
import { Link } from "react-router";
import { useAuth } from "../context/useAuth.js";

export default function HomePage() {
  const { user, logout } = useAuth();

  useEffect(() => {
    document.title = "Início | MeuNegocio360";
  }, []);

  return (
    <div className="app-shell">
      <Navbar
        bg="primary"
        className="site-navbar"
        aria-label="Navegação principal"
      >
        <Container>
          <Navbar.Brand as={Link} to="/">
            MeuNegocio360
          </Navbar.Brand>
          <Button type="button" variant="dark" onClick={logout}>
            Sair
          </Button>
        </Container>
      </Navbar>

      <main className="home-main">
        <Container>
          <Card className="shadow-sm">
            <Card.Body className="p-4 p-md-5">
              <h1 className="h2 mb-2">Olá, {user?.nome}</h1>
              <p className="text-secondary mb-0">
                Seu acesso está ativo como {user?.perfil}.
              </p>
            </Card.Body>
          </Card>
        </Container>
      </main>
    </div>
  );
}
