import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "SaleStorm | High-Concurrency Flash Sale Architecture",
  description: "A showcase of a concurrency-safe flash-sale backend.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.className} bg-[#050505] text-white selection:bg-[#ff3366] selection:text-white`}>
        {children}
      </body>
    </html>
  );
}
