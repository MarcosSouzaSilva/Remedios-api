Documentação do Projeto Spring Boot
Índice
Introdução
Requisitos
Configuração do Ambiente
Estrutura do Projeto
Dependências
Descrição dos Componentes
Controllers
Infraestrutura
Remédio
Usuários
Execução do Projeto
Introdução
Este projeto é um exemplo básico de uma aplicação Spring Boot que oferece funcionalidades de autenticação e gerenciamento de entidades Remédio. A aplicação utiliza Spring Security para a autenticação baseada em JWT.

Requisitos
Java 17 ou superior
Maven 3.2+
IDE (IntelliJ, Eclipse, VS Code, etc.)
Configuração do Ambiente
Instale o JDK: Certifique-se de que o Java Development Kit (JDK) está instalado e configurado no seu PATH.
Instale o Maven: Garanta que o Apache Maven está instalado e configurado no seu PATH.
Configuração da IDE: Configure sua IDE preferida para suportar o desenvolvimento em Spring Boot.
Estrutura do Projeto
A estrutura básica do projeto é a seguinte:
my-spring-boot-project




my-spring-boot-project
│   mvnw
│   mvnw.cmd
│   pom.xml
└───src
    └───main
        ├───java
        │   └───br
        │       └───com
        │           └───marcos
        │               └───projeto
        │                   └───cursospringboot2
        │                       ├───controllers
        │                       ├───infra
        │                       ├───remedio
        │                       └───usuarios
        └───resources
            └───application.properties




            
Dependências
As principais dependências utilizadas neste projeto são:

spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-devtools
lombok
java-jwt
mysql-connector-j
spring-security-test
spring-boot-starter-test
Essas dependências estão definidas no arquivo pom.xml.

Descrição dos Componentes
Controllers
Os controladores são responsáveis por gerenciar as requisições HTTP.

AutenticacaoController
Gerencia as requisições de autenticação dos usuários.

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @PostMapping()
    public ResponseEntity<?> efetuarLogin(@RequestBody @Valid DadosAutenticacao dados) {
        var token = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var autenticacao = manager.authenticate(token);
        var tokenJWT = tokenService.generateToken((Usuario) autenticacao.getPrincipal());
        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
    }
}
RemedioController
Gerencia as operações relacionadas a Remedio.
@RestController
@RequestMapping("/remedios")
public class RemedioController {

    @Autowired
    private RemedioRepository repository;

    @PostMapping
    @Transactional
    public ResponseEntity<DadosDetalhamentoRemedios> cadastrar(@RequestBody @Valid DadosCadastroRemedio dados, UriComponentsBuilder uriBuilder) {
        var remedio = new Remedio(dados);
        repository.save(remedio);
        var uri = uriBuilder.path("/remedios/{id}").buildAndExpand(remedio.getId()).toUri();
        return ResponseEntity.created(uri).body(new DadosDetalhamentoRemedios(remedio));
    }

    @GetMapping
    public ResponseEntity<List<DadosListagemRemedios>> listar() {
        var lista = repository.findAllByAtivoTrue().stream().map(DadosListagemRemedios::new).toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<DadosDetalhamentoRemedios> atualizar(@RequestBody @Valid DadosAtualizarRemedio dados) {
        var remedio = repository.getReferenceById(dados.id());
        remedio.atualizaInformacoes(dados);
        return ResponseEntity.ok(new DadosDetalhamentoRemedios(remedio));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/inativar/{id}")
    @Transactional
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        var remedio = repository.getReferenceById(id);
        remedio.inativar();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ativar/{id}")
    @Transactional
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        var remedio = repository.getReferenceById(id);
        remedio.setAtivar();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DadosDetalhamentoRemedios> buscar(@PathVariable Long id) {
        var remedio = repository.getReferenceById(id);
        return ResponseEntity.ok(new DadosDetalhamentoRemedios(remedio));
    }
}
Infraestrutura
Gerencia a configuração de segurança, manipulação de tokens e tratamento de erros.

SecurityConfiguarations
Configura as regras de segurança da aplicação.
@Configuration
@EnableWebSecurity
public class SecurityConfiguarations {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(custom -> custom.disable())
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(customizer -> customizer.requestMatchers(HttpMethod.POST, "/login").permitAll().anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
@Configuration
@EnableWebSecurity
public class SecurityConfiguarations {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(custom -> custom.disable())
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(customizer -> customizer.requestMatchers(HttpMethod.POST, "/login").permitAll().anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
'TokenService'
Gera e valida tokens JWT.
@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(Usuario usuario) {
        try {
            var algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("Remedios-api")
                    .withSubject(usuario.getLogin())
                    .withExpiresAt(dataExpiracao())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token !", exception);
        }
    }

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    public String getSubject(String token) {
        try {
            var algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("Remedios-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Token inválido ou expirado !");
        }
    }
}
SecurityFilter
Filtra as requisições para verificar a validade do token.
@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var tokenJWT = recuperarToken(request);
        if (tokenJWT != null) {
            var subject = tokenService.getSubject(tokenJWT);
            var usuario = repository.findByLogin(subject);
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorization = request.getHeader("Authorization");
        if (authorization != null) {
            return authorization.replace("Bearer ", "");
        }
        return null;
    }
}
'TratadorDeErros'
Trata as exceções lançadas pela aplicação.
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> tratador404() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> tratador400(MethodArgumentNotValidException ex) {
        var erros = ex.getFieldErrors();
        return ResponseEntity.badRequest().body(erros.stream().map(DadosErros::new).toList());
   
