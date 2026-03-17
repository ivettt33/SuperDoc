import React from "react";

export function Card({ children }: { children: React.ReactNode }) {
  return (
    <div style={{
      background: "white",
      borderRadius: 12,
      padding: 20,
      boxShadow: "0 4px 16px rgba(0,0,0,0.08)",
      width: "100%",
    }}>
      {children}
    </div>
  );
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      style={{
        padding: "10px 12px",
        borderRadius: 8,
        border: "1px solid #d1d5db",
        outline: "none",
        width: "100%",
      }}
    />
  );
}

export function Button({ children, ...rest }: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      {...rest}
      style={{
        padding: "10px 12px",
        borderRadius: 8,
        border: "1px solid #111827",
        background: "#111827",
        color: "white",
        cursor: "pointer",
      }}
    >
      {children}
    </button>
  );
}


