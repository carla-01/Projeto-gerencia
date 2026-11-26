# FilaSUS - teste de queda do servidor (SCRUM-63 · Driver DA-DIS-I2-01)
#
# Cria uma fila com pacientes fictícios, derruba o back-end de propósito, sobe de novo,
# cronometra até o /actuator/health responder UP e confere se as senhas continuam iguais.
#
# Antes de rodar:
#   1. PostgreSQL ligado.
#   2. Gerar o .jar e subir o back-end, num terminal à parte, na pasta backend:
#        mvn -B package -DskipTests
#        java -jar target/filasus-0.1.0-SNAPSHOT.jar
#      (java do JDK 21 ou mais novo). O mvn spring-boot:run não achou a classe principal
#      numa pasta com acento no caminho, por isso o teste usa o .jar.
#   3. Fazer o teste com a QA acompanhando.
#
# Como rodar, na raiz do repositório:
#   powershell -ExecutionPolicy Bypass -File docs/devops/queda/teste-queda-servidor.ps1
# O psql pede a senha do banco; para não pedir a cada consulta, definir $env:PGPASSWORD antes.
#
# O back-end sobe de novo com as mesmas variáveis de ambiente deste terminal (DB_URL,
# CPF_CHAVE_CIFRA e as outras). Use o mesmo terminal e as mesmas variáveis da primeira subida.

param(
    [string]$Psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe",
    [string]$Banco = "filasus",
    [string]$UsuarioDoBanco = "filasus",
    [int]$PortaDoBanco = 5432,
    # Desliga o back-end no fim (sem isso ele continua no ar).
    [switch]$DesligarNoFim
)

$ErrorActionPreference = "Stop"
$Api = "http://localhost:8080"
$PortaDaApi = 8080
$QuantidadeDePacientes = 5
$MetaEmSegundos = 120
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$pastaDoBackend = Join-Path $raiz "backend"
$logDoBackend = Join-Path $env:TEMP "filasus-queda-backend.log"
$jar = "target\filasus-0.1.0-SNAPSHOT.jar"
$java = "java"
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
}

function Consultar([string]$sql) {
    $saida = & $Psql -h localhost -p $PortaDoBanco -U $UsuarioDoBanco -d $Banco -v ON_ERROR_STOP=1 -t -A -c $sql
    if ($LASTEXITCODE -ne 0) {
        throw "O psql falhou. Confira o banco e a senha."
    }
    return $saida
}

# true quando o servidor responde qualquer coisa. Sem login o /actuator/health devolve 401
# (o SecurityConfig não libera essa rota), e mesmo assim isso mostra que a API está no ar.
function ApiNoAr {
    try {
        Invoke-WebRequest -Uri "$Api/actuator/health" -UseBasicParsing -TimeoutSec 2 | Out-Null
        return $true
    } catch {
        return ($null -ne $_.Exception.Response)
    }
}

# true quando o /actuator/health, consultado com um login de teste, responde UP.
function SaudeUp([string]$token) {
    try {
        $saude = Invoke-RestMethod -Uri "$Api/actuator/health" -Headers @{ Authorization = "Bearer $token" } -TimeoutSec 2
        return ($saude.status -eq "UP")
    } catch {
        return $false
    }
}

function Enviar([string]$caminho, $corpo, [string]$token) {
    $cabecalhos = @{}
    if ($token) {
        $cabecalhos["Authorization"] = "Bearer $token"
    }
    $json = $corpo | ConvertTo-Json
    return Invoke-RestMethod -Method Post -Uri "$Api$caminho" -Headers $cabecalhos `
        -ContentType "application/json" -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
}

# CPF fictício com os dois dígitos verificadores certos.
function NovoCpf([int]$semente) {
    $cpf = "{0:D9}" -f (630000000 + $semente)
    foreach ($quantidade in 9, 10) {
        $soma = 0
        for ($i = 0; $i -lt $quantidade; $i++) {
            $soma += [int]::Parse($cpf[$i]) * ($quantidade + 1 - $i)
        }
        $digito = ($soma * 10) % 11
        if ($digito -eq 10) {
            $digito = 0
        }
        $cpf = $cpf + $digito
    }
    return $cpf
}

# Senha de teste sorteada a cada execução: nenhuma senha fica gravada no arquivo.
$senhaDeTeste = "Qd-" + [guid]::NewGuid().ToString("N").Substring(0, 12)

function CadastrarEEntrar([string]$email, [int]$semente) {
    Enviar "/pacientes/autocadastro" @{
        nome = "Paciente Ficticio Queda $semente"; cpf = (NovoCpf $semente)
        cartaoSus = "89800000000{0:D4}" -f $semente; email = $email; senha = $senhaDeTeste; aceiteTermo = $true
    } "" | Out-Null
}

function Login([string]$email) {
    return (Enviar "/auth/login" @{ login = $email; senha = $senhaDeTeste } "").token
}

function ApagarDadosDoTeste {
    $filtro = "SELECT id FROM paciente WHERE email LIKE 'queda.%@exemplo.test'"
    Consultar ("BEGIN; " +
        "DELETE FROM senha WHERE paciente_id IN ($filtro); " +
        "DELETE FROM consentimento WHERE paciente_id IN ($filtro); " +
        "DELETE FROM paciente WHERE email LIKE 'queda.%@exemplo.test'; " +
        "DELETE FROM usuario WHERE email LIKE 'queda.%@exemplo.test'; " +
        "DELETE FROM configuracao_servico WHERE servico_id IN (SELECT id FROM servico WHERE nome = 'Teste de Queda'); " +
        "DELETE FROM servico WHERE nome = 'Teste de Queda'; " +
        "COMMIT;") | Out-Null
}

$consultaDaFila = "SELECT s.id, s.numero, s.status FROM senha s JOIN servico v ON v.id = s.servico_id " +
    "WHERE v.nome = 'Teste de Queda' ORDER BY s.id"

if (-not (Test-Path (Join-Path $pastaDoBackend $jar))) {
    throw "Falta o $jar. Na pasta backend, rode: mvn -B package -DskipTests"
}
if (-not (ApiNoAr)) {
    throw "O back-end não está no ar em $Api. Suba antes (docs/devops/homologacao.txt)."
}

Write-Host "1. Preparando a fila com $QuantidadeDePacientes pacientes fictícios..."
ApagarDadosDoTeste

CadastrarEEntrar "queda.gestor@exemplo.test" 0
Consultar "UPDATE usuario SET perfil = 'GESTOR' WHERE email = 'queda.gestor@exemplo.test'" | Out-Null
$tokenDoGestor = Login "queda.gestor@exemplo.test"
$servico = Enviar "/api/gestor/servicos" @{
    nome = "Teste de Queda"; horarioAbertura = "00:00"; horarioFechamento = "23:59"; limiteSenhasDia = 20
} $tokenDoGestor
$servicoId = (Consultar "SELECT id FROM servico WHERE nome = 'Teste de Queda'")

$tokens = @()
for ($n = 1; $n -le $QuantidadeDePacientes; $n++) {
    CadastrarEEntrar "queda.paciente$n@exemplo.test" $n
    $token = Login "queda.paciente$n@exemplo.test"
    Enviar "/api/paciente/senhas" @{ servicoId = [long]$servicoId } $token | Out-Null
    $tokens += $token
}

if (-not (SaudeUp $tokens[0])) {
    throw "O /actuator/health não respondeu UP antes da queda."
}
$antes = @(Consultar $consultaDaFila)
$posicaoAntes = (Invoke-RestMethod -Uri "$Api/api/paciente/senhas" `
    -Headers @{ Authorization = "Bearer $($tokens[-1])" })[0].posicao
Write-Host "   Senhas antes da queda (id|numero|status):"
$antes | ForEach-Object { Write-Host "     $_" }
Write-Host "   Posição do último paciente: $posicaoAntes"

Write-Host "2. Derrubando o back-end..."
$processos = Get-NetTCPConnection -LocalPort $PortaDaApi -State Listen |
    Select-Object -ExpandProperty OwningProcess -Unique
$inicio = Get-Date
$cronometro = [System.Diagnostics.Stopwatch]::StartNew()
foreach ($id in $processos) {
    Stop-Process -Id $id -Force
}
Start-Sleep -Seconds 2
if (ApiNoAr) {
    throw "O back-end continuou respondendo: a queda não aconteceu."
}
Write-Host "   Back-end fora do ar às $($inicio.ToString('HH:mm:ss'))."

Write-Host "3. Subindo de novo (java -jar) e cronometrando..."
Start-Process -FilePath $java -ArgumentList "-jar", $jar -WorkingDirectory $pastaDoBackend `
    -WindowStyle Hidden -RedirectStandardOutput $logDoBackend
while (-not (SaudeUp $tokens[0])) {
    if ($cronometro.Elapsed.TotalSeconds -gt 300) {
        throw "O back-end não voltou em 5 minutos. Veja o log em $logDoBackend"
    }
    Start-Sleep -Seconds 1
}
$cronometro.Stop()
$segundos = [math]::Round($cronometro.Elapsed.TotalSeconds, 1)
Write-Host "   API respondeu UP depois de $segundos segundos."

Write-Host "4. Conferindo as senhas..."
$depois = @(Consultar $consultaDaFila)
$posicaoDepois = (Invoke-RestMethod -Uri "$Api/api/paciente/senhas" `
    -Headers @{ Authorization = "Bearer $($tokens[-1])" })[0].posicao
$depois | ForEach-Object { Write-Host "     $_" }
Write-Host "   Posição do último paciente: $posicaoDepois"

$senhasIguais = (($antes -join ";") -eq ($depois -join ";"))
$noPrazo = ($segundos -le $MetaEmSegundos)
$atendeu = "Não"
if ($senhasIguais -and $noPrazo -and ($posicaoAntes -eq $posicaoDepois)) {
    $atendeu = "Sim"
}

Write-Host ""
Write-Host "RESULTADO ($((Get-Date).ToString('dd/MM/yyyy HH:mm')))"
Write-Host "  Tempo até voltar:       $segundos s (meta: até $MetaEmSegundos s)"
Write-Host "  Senhas antes / depois:  $($antes.Count) / $($depois.Count) (iguais: $senhasIguais)"
Write-Host "  Posição antes / depois: $posicaoAntes / $posicaoDepois"
Write-Host "  Atendeu?                $atendeu"

ApagarDadosDoTeste
Write-Host "Dados fictícios do teste apagados."

if ($DesligarNoFim) {
    Get-NetTCPConnection -LocalPort $PortaDaApi -State Listen |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object { Stop-Process -Id $_ -Force }
    Write-Host "Back-end desligado."
} else {
    Write-Host "O back-end continua no ar em segundo plano (log em $logDoBackend)."
}
