package com.clientes_api.config;

import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public void run(String... args) throws Exception {
        // Verifica se o banco de dados de usuários está vazio
        if (repository.count() == 0) {
            System.out.println("Banco de usuários vazio. Criando administrador padrão...");
            
            // Criptografa a senha "123" usando o algoritmo BCrypt
            String encryptedPassword = new BCryptPasswordEncoder().encode("123");
            
            // Cria o usuário com e-mail, nome de usuário, senha criptografada e nível ADMIN
            Usuario admin = new Usuario("admin@lexcrm.com.br", encryptedPassword, UsuarioRole.ADMIN);
            admin.setUsername("admin.lex");
            
            // Salva no banco de dados MySQL
            repository.save(admin);
            
            System.out.println("✅ Usuário criado com sucesso!");
            System.out.println("➡️ E-mail: admin@lexcrm.com.br");
            System.out.println("➡️ Username: admin.lex");
            System.out.println("➡️ Senha: 123");
        }
    }
}
