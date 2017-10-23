package edu.dnatools.conf;

import edu.dnatools.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.http.HttpMethod.*;


@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
       // auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
             auth.authenticationProvider(authenticationProvider());
    }

    public void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and().csrf().disable().authorizeRequests()
                .antMatchers(PUT, "/submit").hasAnyRole("USER", "ADMIN")
                .antMatchers(PUT, "/addprotein").hasAnyRole("USER", "ADMIN")
                .antMatchers(PUT, "/addstructure").hasAnyRole("USER", "ADMIN")
                .antMatchers(DELETE, "/delete/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(DELETE, "/deleteprotein/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(GET, "/conf/userlist").hasRole("ADMIN")
                .antMatchers(GET, "/myjobs").hasAnyRole("USER", "ADMIN")
                .antMatchers(GET, "/myjobs/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(GET, "/steps/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().permitAll();
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
