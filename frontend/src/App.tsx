import { useEffect, useState } from "react";
import "./App.css";

type Page = "register" | "login" | "dashboard";

interface CurrentUser {
  id: number;
  name: string;
  email: string;
}

function App() {
  // ============================================================
  // REGISTER STATE
  // ============================================================

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // ============================================================
  // LOGIN STATE
  // ============================================================

  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");

  // ============================================================
  // APPLICATION STATE
  // ============================================================

  const [page, setPage] = useState<Page>("register");

  const [currentUser, setCurrentUser] =
    useState<CurrentUser | null>(null);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // ============================================================
  // CHECK EXISTING SESSION
  // ============================================================
  //
  // When the React application starts, ask the backend:
  //
  // "Does this browser already have an authenticated session?"
  //
  // We do NOT store JSESSIONID ourselves.
  // The browser manages the cookie.
  //
  // ============================================================

  useEffect(() => {
    const checkSession = async () => {
      try {
        const response = await fetch(
          "http://localhost:8080/api/auth/me",
          {
            credentials: "include",
          }
        );

        if (!response.ok) {
          return;
        }

        const user: CurrentUser = await response.json();

        setCurrentUser(user);
        setPage("dashboard");
      } catch {
        // No active session.
        // Stay on the current page.
      }
    };

    checkSession();
  }, []);

  // ============================================================
  // REGISTER
  // ============================================================

  const handleRegister = async (
    event: React.FormEvent
  ) => {
    event.preventDefault();

    setMessage("");
    setError("");

    // Client-side validation
    if (
      !name ||
      !email ||
      !password ||
      !confirmPassword
    ) {
      setError("All fields are required.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    if (password.length < 12) {
      setError(
        "Password must be at least 12 characters."
      );
      return;
    }

    try {
      setLoading(true);

      const response = await fetch(
        "http://localhost:8080/api/users",
        {
          method: "POST",

          headers: {
            "Content-Type": "application/json",
          },

          body: JSON.stringify({
            name,
            email,
            password,
          }),
        }
      );

      const contentType =
        response.headers.get("content-type");

      let data: any;

      if (
        contentType &&
        contentType.includes("application/json")
      ) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (!response.ok) {
        // Our backend validation response looks like:
        //
        // {
        //   "status": 400,
        //   "message": "Validation failed",
        //   "errors": {
        //      "password": "Password must be..."
        //   }
        // }

        if (
          data &&
          typeof data === "object" &&
          data.errors
        ) {
          const validationMessages =
            Object.values(data.errors);

          throw new Error(
            validationMessages.join(" ")
          );
        }

        throw new Error(
          data?.message ||
            data ||
            "Registration failed."
        );
      }

      setMessage(
        "Account created successfully. You can now login."
      );

      // Clear registration form
      setName("");
      setEmail("");
      setPassword("");
      setConfirmPassword("");

      // Move user to login page
      setPage("login");
    } catch (error) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("Something went wrong.");
      }
    } finally {
      setLoading(false);
    }
  };

  // ============================================================
  // LOGIN
  // ============================================================

  const handleLogin = async (
    event: React.FormEvent
  ) => {
    event.preventDefault();

    setMessage("");
    setError("");

    if (!loginEmail || !loginPassword) {
      setError("Email and password are required.");
      return;
    }

    try {
      setLoading(true);

      // --------------------------------------------------------
      // Step 1: Login
      // --------------------------------------------------------

      const response = await fetch(
        "http://localhost:8080/api/auth/login",
        {
          method: "POST",

          // VERY IMPORTANT
          //
          // Allows the browser to receive/send
          // the JSESSIONID cookie.
          credentials: "include",

          headers: {
            "Content-Type": "application/json",
          },

          body: JSON.stringify({
            email: loginEmail,
            password: loginPassword,
          }),
        }
      );

      const data = await response.text();

      if (!response.ok) {
        throw new Error(
          data || "Login failed."
        );
      }

      // --------------------------------------------------------
      // Step 2: Ask backend who is logged in
      // --------------------------------------------------------

      const meResponse = await fetch(
        "http://localhost:8080/api/auth/me",
        {
          credentials: "include",
        }
      );

      if (!meResponse.ok) {
        throw new Error(
          "Login succeeded, but user session could not be verified."
        );
      }

      const user: CurrentUser =
        await meResponse.json();

      // --------------------------------------------------------
      // Step 3: Store user only in React memory
      // --------------------------------------------------------

      setCurrentUser(user);

      // --------------------------------------------------------
      // Step 4: Move to dashboard
      // --------------------------------------------------------

      setPage("dashboard");

      // Clear login form
      setLoginEmail("");
      setLoginPassword("");
    } catch (error) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("Something went wrong.");
      }
    } finally {
      setLoading(false);
    }
  };

  // ============================================================
  // LOGOUT
  // ============================================================

  const handleLogout = async () => {
    setMessage("");
    setError("");

    try {
      setLoading(true);

      const response = await fetch(
        "http://localhost:8080/api/auth/logout",
        {
          method: "POST",
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error("Logout failed.");
      }

      // Clear React state
      setCurrentUser(null);

      setLoginEmail("");
      setLoginPassword("");

      // Move to login page
      setPage("login");

      setMessage("Logout successful.");
    } catch (error) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("Something went wrong.");
      }
    } finally {
      setLoading(false);
    }
  };

  // ============================================================
  // LOGIN PAGE
  // ============================================================

  if (page === "login") {
    return (
      <main className="app">
        <section className="card">
          <h1>SecureBank</h1>

          <h2>Login</h2>

          <form onSubmit={handleLogin}>
            <label htmlFor="loginEmail">
              Email
            </label>

            <input
              id="loginEmail"
              type="email"
              placeholder="Enter your email"
              value={loginEmail}
              onChange={(event) =>
                setLoginEmail(event.target.value)
              }
              autoComplete="email"
            />

            <label htmlFor="loginPassword">
              Password
            </label>

            <input
              id="loginPassword"
              type="password"
              placeholder="Enter your password"
              value={loginPassword}
              onChange={(event) =>
                setLoginPassword(event.target.value)
              }
              autoComplete="current-password"
            />

            {error && (
              <p className="error">
                {error}
              </p>
            )}

            {message && (
              <p className="success">
                {message}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Logging in..."
                : "Login"}
            </button>
          </form>

          <p className="login-link">
            Don't have an account?{" "}

            <button
              type="button"
              className="link-button"
              onClick={() => {
                setPage("register");
                setError("");
                setMessage("");
              }}
            >
              Create Account
            </button>
          </p>
        </section>
      </main>
    );
  }

  // ============================================================
  // DASHBOARD PAGE
  // ============================================================

  if (page === "dashboard") {
    return (
      <main className="app">
        <section className="card">
          <h1>SecureBank</h1>

          <h2>Dashboard</h2>

          {currentUser && (
            <div className="user-info">
              <p>
                <strong>Welcome:</strong>{" "}
                {currentUser.name}
              </p>

              <p>
                <strong>User ID:</strong>{" "}
                {currentUser.id}
              </p>

              <p>
                <strong>Email:</strong>{" "}
                {currentUser.email}
              </p>
            </div>
          )}

          {error && (
            <p className="error">
              {error}
            </p>
          )}

          {message && (
            <p className="success">
              {message}
            </p>
          )}

          <button
            type="button"
            onClick={handleLogout}
            disabled={loading}
          >
            {loading
              ? "Logging out..."
              : "Logout"}
          </button>
        </section>
      </main>
    );
  }

  // ============================================================
  // REGISTER PAGE
  // ============================================================

  return (
    <main className="app">
      <section className="card">
        <h1>SecureBank</h1>

        <h2>Create Account</h2>

        <form onSubmit={handleRegister}>
          <label htmlFor="name">
            Name
          </label>

          <input
            id="name"
            type="text"
            placeholder="Enter your name"
            value={name}
            onChange={(event) =>
              setName(event.target.value)
            }
            autoComplete="name"
          />

          <label htmlFor="email">
            Email
          </label>

          <input
            id="email"
            type="email"
            placeholder="Enter your email"
            value={email}
            onChange={(event) =>
              setEmail(event.target.value)
            }
            autoComplete="email"
          />

          <label htmlFor="password">
            Password
          </label>

          <input
            id="password"
            type="password"
            placeholder="Enter your password"
            value={password}
            onChange={(event) =>
              setPassword(event.target.value)
            }
            autoComplete="new-password"
          />

          <label htmlFor="confirmPassword">
            Confirm Password
          </label>

          <input
            id="confirmPassword"
            type="password"
            placeholder="Confirm your password"
            value={confirmPassword}
            onChange={(event) =>
              setConfirmPassword(event.target.value)
            }
            autoComplete="new-password"
          />

          {error && (
            <p className="error">
              {error}
            </p>
          )}

          {message && (
            <p className="success">
              {message}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
          >
            {loading
              ? "Creating..."
              : "Create Account"}
          </button>
        </form>

        <p className="login-link">
          Already have an account?{" "}

          <button
            type="button"
            className="link-button"
            onClick={() => {
              setPage("login");
              setMessage("");
              setError("");
            }}
          >
            Login
          </button>
        </p>
      </section>
    </main>
  );
}

export default App;