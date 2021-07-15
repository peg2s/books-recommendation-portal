package com.peg2s.service;

import com.peg2s.models.User;
import com.peg2s.models.enums.Role;
import com.peg2s.models.enums.Sex;
import com.peg2s.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.Random;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final MailService mailService;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        return userRepository.findByLoginIgnoreCase(s);
    }

    public void updateProfile(User user) {
        String pass = user.getPassword();
        user = userRepository.findById(user.getId()).get();
        user = user.toBuilder()
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .login(user.getLogin())
                .password(passwordEncoder.encode(pass))
                .sex(user.getSex())
                .build();
        userRepository.save(user);
    }

    public void updateUserProfileByAdmin(User user, String id, String isAdmin) {
        String pass = user.getPassword();

        User userFromDb = userRepository.findById(Long.valueOf(id)).get();
        if (!pass.equals(userFromDb.getPassword())) {
            userFromDb.setPassword(passwordEncoder.encode(pass));
        }
        if (isAdmin != null && !userFromDb.getRoles().contains(Role.ROLE_ADMIN)) {
            userFromDb.getRoles().add(Role.ROLE_ADMIN);
        } else if (isAdmin == null) {
            userFromDb.getRoles().remove(Role.ROLE_ADMIN);
        }
        userFromDb.setDateOfBirth(user.getDateOfBirth());
        userFromDb.setLogin(user.getLogin());
        userRepository.save(userFromDb);
    }

    public String registerUser(User user, Model model) {
        User registeredUser = userRepository.findByLoginIgnoreCase(user.getLogin());
        if (!user.checkInputOk()) {
            model.addAttribute("warning", "Все поля формы обязательны к заполнению!");
            return "register";
        } else if (registeredUser != null) {
            model.addAttribute("warning", "Пользователь с таким логином существует!");
            return "register";
        }
        user.setActivationCode(UUID.randomUUID().toString());
        mailService.send(user.getEmail(), "Уведомление об успешной регистрации",
                String.format("%s %s, добро пожаловать на наш портал! \n" +
                                "\nМы очень рады что вы к нам присоединились и очень постараемся радовать вас новой и полезной информацией! \n\n" +
                                "Для подтверждения регистрации перейдите по следующей ссылке:\n https://lit-hamlet-12359.herokuapp.com/common/activate?id=%s&code=%s" +
                                "\n\n" +
                                "Отвечать на данное письмо не нужно, так как оно отправлено вежливым, но очень стеснительным роботом.",
                        user.getSex().equals(Sex.MALE) ? "Уважаемый" : "Уважаемая",
                        user.getFullName(),
                        user.getLogin(),
                        user.getActivationCode()));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(Role.ROLE_USER);
        userRepository.save(user);
        model.addAttribute("warning", "Вы успешно зарегистрированы. Ваш логин " + user.getLogin() + ".\n" +
                "Проверьте указанный адрес электронной почты для активации профиля.");
        return "register";
    }

    public boolean activateUser(String userLogin, String code) {
        User user = userRepository.findByLoginIgnoreCase(userLogin);
        if (user != null) {
            if (user.getActivationCode().equals(code)) {
                user.setActive(true);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public String getPasswordResetLink(Model model, String login) {
        User user = userRepository.findByLoginIgnoreCase(login);
        if (user != null) {
            user.setPasswordResetCode(UUID.randomUUID().toString());
            userRepository.save(user);
            mailService.send(user.getEmail(), "Ссылка для сброса пароля",
                    String.format("%s %s, вы или кто-то от вашего имени запросил сброс пароля к вашей учетной записи. \n" +
                                    "\nЕсли вы не запрашивали сброс пароля, то свяжитесь с администратором в telegram: @peg2sus \n\n" +
                                    "Для сброса пароля к учетной записи перейдите по ссылке:\n https://lit-hamlet-12359.herokuapp.com/common/resetPassword?id=%s&code=%s" +
                                    "\n\n" +
                                    "Отвечать на данное письмо не нужно, так как оно отправлено вежливым, но очень стеснительным роботом.",
                            user.getSex().equals(Sex.MALE) ? "Уважаемый" : "Уважаемая",
                            user.getFullName(),
                            user.getLogin(),
                            user.getPasswordResetCode()));
            model.addAttribute("loginNotification", "Ссылка для сброса пароля отправлена на почтовый ящик, указанный при регистрации.");
        } else {
            model.addAttribute("loginNotification", "Пользователь с таким логином не найден.");
            return "login";
        }
        return "login";
    }

    public String resetPassword(String userLogin, String verificationCode, Model model) {
        User user = userRepository.findByLoginIgnoreCase(userLogin);
        if (user != null && user.getPasswordResetCode() != null) {
            if (user.getPasswordResetCode().equals(verificationCode)
                    && user.getPasswordResetCode() != null) {

                String password = new Random().ints(10, 33, 122).collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();

                user.setPassword(passwordEncoder.encode(password));
                mailService.send(user.getEmail(), "Ваш временный пароль для доступа на портал.",
                        String.format("%s %s, пароль к вашей учетной записи сброшен. Ваш новый пароль: %s \n" +
                                        "Смените его при первой же возможности. \n\n" +
                                        "Отвечать на данное письмо не нужно, так как оно отправлено вежливым, но очень стеснительным роботом.",
                                user.getSex().equals(Sex.MALE) ? "Уважаемый" : "Уважаемая",
                                user.getFullName(),
                                password));
                user.setPasswordResetCode(null);
                userRepository.save(user);
                model.addAttribute("loginNotification", "Пароль успешно сброшен. " +
                        "<br>" +
                        "Временный пароль отправлен вам на почту. Смените его при первой возможности.");
                return "login";
            }
        }
        model.addAttribute("loginNotification", "Что-то пошло не так. Попробуйте запросить сброс повторно.");
        return "login";
    }
}
