package com.library.booksystem.controller;

import com.library.booksystem.entity.ERole;
import com.library.booksystem.entity.Role;
import com.library.booksystem.entity.User;
import com.library.booksystem.repository.RoleRepository;
import com.library.booksystem.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashSet;
import java.util.Set;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute User user,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        // Проверка существующего пользователя
        if (userRepository.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Имя пользователя уже занято");
            return "register";
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email уже используется");
            return "register";
        }

        // Создание нового пользователя
        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setFullName(user.getFullName());
        newUser.setStudentId(user.getStudentId());
        newUser.setFaculty(user.getFaculty());
        newUser.setPhoneNumber(user.getPhoneNumber());

        // Назначение роли STUDENT по умолчанию
        Set<Role> roles = new HashSet<>();
        Role studentRole = roleRepository.findByName(ERole.ROLE_STUDENT)
                .orElseThrow(() -> new RuntimeException("Роль не найдена"));
        roles.add(studentRole);
        newUser.setRoles(roles);

        userRepository.save(newUser);

        return "redirect:/login?registered=true";
    }
}