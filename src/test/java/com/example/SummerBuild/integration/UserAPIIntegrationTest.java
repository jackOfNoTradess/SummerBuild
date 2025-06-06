import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestAuthConfig.class, TestSecurityConfig.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class UserAPIIntegrationTest {

    @MockBean
    private UserService userService;

    @BeforeEach
    void seedTestDataAndSetupMocks() {
        // Clear existing data
        userRepository.deleteAll();

        // Create test users
        userRepository.saveAll(
            List.of(
                // User Role
                User.builder()
                    .id(UUID.randomUUID())
                    .role(UserRole.USER)
                    .gender(Gender.MALE)
                    .build(),

                // Admin Role
                User.builder()
                    .id(UUID.randomUUID())
                    .role(UserRole.ADMIN)
                    .gender(Gender.FEMALE)
                    .build()));

        // Mock getUserById to use the local database
        when(userService.getUserById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return userRepository.findById(id)
                .map(user -> {
                    String response = String.format(
                        "{\"id\":\"%s\",\"role\":\"%s\",\"gender\":\"%s\"}",
                        user.getId(),
                        user.getRole(),
                        user.getGender()
                    );
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
        });

        // Mock findByRole to use the local database
        when(userService.findByRole(any(UserRole.class))).thenAnswer(invocation -> {
            UserRole role = invocation.getArgument(0);
            return userRepository.findByRole(role).stream()
                .map(user -> new UserDto(user.getId(), user.getRole(), user.getGender(), user.getCreatedAt(), user.getUpdatedAt()))
                .toList();
        });
    }
} 