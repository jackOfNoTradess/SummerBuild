// package com.example.SummerBuild.repository;

// import static org.assertj.core.api.Assertions.assertThat;

// import com.example.SummerBuild.model.Gender;
// import com.example.SummerBuild.model.User;
// import com.example.SummerBuild.model.UserRole;
// import java.util.Optional;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
// import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

// @DataJpaTest
// public class UserRepositoryTest {

//   @Autowired private TestEntityManager entityManager;

//   @Autowired private UserRepository userRepository;

//   @Test
//   void whenSaveUser_thenReturnSavedUser() {
//     // Given
//     User user = new User();
//     user.setName("testuser");
//     user.setEmail("test@example.com");
//     user.setRole(UserRole.USER);
//     user.setGender(Gender.MALE);

//     // When
//     User savedUser = userRepository.save(user);

//     // Then
//     assertThat(savedUser).isNotNull();
//     assertThat(savedUser.getId()).isNotNull();
//     assertThat(savedUser.getName()).isEqualTo("testuser");
//   }

//   @Test
//   void whenFindByName_thenReturnUser() {
//     // Given
//     User user = new User();
//     user.setName("testuser");
//     user.setEmail("test@example.com");
//     user.setRole(UserRole.USER);
//     user.setGender(Gender.MALE);
//     entityManager.persist(user);
//     entityManager.flush();

//     // When
//     Optional<User> found = userRepository.findByName("testuser");

//     // Then
//     assertThat(found).isPresent();
//     assertThat(found.get().getName()).isEqualTo("testuser");
//   }

//   @Test
//   void whenFindByEmail_thenReturnUser() {
//     // Given
//     User user = new User();
//     user.setName("testuser");
//     user.setEmail("test@example.com");
//     user.setRole(UserRole.USER);
//     user.setGender(Gender.MALE);
//     entityManager.persist(user);
//     entityManager.flush();

//     // When
//     Optional<User> found = userRepository.findByEmail("test@example.com");

//     // Then
//     assertThat(found).isPresent();
//     assertThat(found.get().getEmail()).isEqualTo("test@example.com");
//   }
// }
