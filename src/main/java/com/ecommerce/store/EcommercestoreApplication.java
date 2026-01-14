package com.ecommerce.store;

import com.ecommerce.store.entities.Role;
import com.ecommerce.store.entities.User;
import com.ecommerce.store.repositories.RoleRepository;
import com.ecommerce.store.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

@SpringBootApplication
public class EcommercestoreApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(EcommercestoreApplication.class, args);
    }

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository repository;
    @Value("${normal.role.id}")
    private String role_normal_id;
    @Value("${admin.role.id}")
    private String role_admin_id;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {

        System.out.println(passwordEncoder.encode("abcd"));

        try {

            Role role_admin = Role.builder().roleId(role_admin_id).roleName("ROLE_ADMIN").build();
            Role role_normal = Role.builder().roleId(role_normal_id).roleName("ROLE_NORMAL").build();

            if (repository.findById(role_admin_id).isEmpty()) {
                repository.save(role_admin);
            }
            if (repository.findById(role_normal_id).isEmpty()) {
                repository.save(role_normal);
            }

            if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
                User adminUser = User.builder()
                        .name("admin")
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("admin123"))
                        .gender("Male")
                        .imageName("default.png")
                        .roles(Set.of(role_admin, role_normal))
                        .userId(UUID.randomUUID().toString())
                        .about("I am admin User")
                        .build();
                userRepository.save(adminUser);
            }

            if (userRepository.findByEmail("durgesh@gmail.com").isEmpty()) {
                User normalUser = User.builder()
                        .name("durgesh")
                        .email("durgesh@gmail.com")
                        .password(passwordEncoder.encode("durgesh123"))
                        .gender("Male")
                        .imageName("default.png")
                        .roles(Set.of(role_normal))
                        .userId(UUID.randomUUID().toString())
                        .about("I am Normal User")
                        .build();
                userRepository.save(normalUser);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
