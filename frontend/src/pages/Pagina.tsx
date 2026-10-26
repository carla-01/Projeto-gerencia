type Props = {
  titulo: string
  descricao: string
}

/** Página provisória do esqueleto. Cada história substitui pela tela real. */
export default function Pagina({ titulo, descricao }: Props) {
  return (
    <main>
      <h1>{titulo}</h1>
      <p>{descricao}</p>
    </main>
  )
}
