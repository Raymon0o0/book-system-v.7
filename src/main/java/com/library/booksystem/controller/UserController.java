package com.library.booksystem.controller;

import com.library.booksystem.entity.Role;
import com.library.booksystem.entity.User;
import com.library.booksystem.repository.RoleRepository;
import com.library.booksystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "user-list";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("user", user);
        return "user-details";
    }

    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<Role> allRoles = roleRepository.findAll();

        model.addAttribute("user", user);
        model.addAttribute("allRoles", allRoles);
        return "user-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             @RequestParam(required = false) List<Long> roleIds,
                             RedirectAttributes redirectAttributes) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setStudentId(user.getStudentId());
        existingUser.setFaculty(user.getFaculty());
        existingUser.setPhoneNumber(user.getPhoneNumber());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Обновление ролей
        if (roleIds != null) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : roleIds) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("Роль не найдена"));
                roles.add(role);
            }
            existingUser.setRoles(roles);
        }

        userRepository.save(existingUser);
        redirectAttributes.addFlashAttribute("success", "Пользователь обновлен");
        return "redirect:/users/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Пользователь удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
        }
        return "redirect:/users";
    }
}