package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.smtp.command.*;
import de.gessnerfl.fakesmtp.smtp.server.RequireAuthCommandWrapper;
import de.gessnerfl.fakesmtp.smtp.server.RequireTLSCommandWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmtpCommandConfig {

    @Bean
    public CommandHandler commandHandler(){
        return new CommandHandler(commandRegistry());
    }

    @Bean
    public CommandRegistry commandRegistry() {
        return new CommandRegistry(authCommand(),
                dataCommand(),
                ehloCommand(),
                helloCommand(),
                helpCommand(),
                mailCommand(),
                noopCommand(),
                quitCommand(),
                receiptCommand(),
                resetCommand(),
                startTLSCommand(),
                verifyCommand(),
                expandCommand());
    }

    @Bean
    public Command authCommand() {
        return withTlsCheckWhenRequired(new AuthCommand());
    }

    @Bean
    public Command dataCommand() {
        return withAuthCheckWhenRequired(withTlsCheckWhenRequired(new DataCommand()));
    }

    public Command ehloCommand() {
        return new EhloCommand();
    }

    @Bean
    public Command helloCommand() {
        return withTlsCheckWhenRequired(new HelloCommand());
    }

    @Bean
    public Command helpCommand() {
        return withAuthCheckWhenRequired(withTlsCheckWhenRequired(new HelpCommand()));
    }

    @Bean
    public Command mailCommand() {
        return withAuthCheckWhenRequired(withTlsCheckWhenRequired(new MailCommand()));
    }

    @Bean
    public Command noopCommand() {
        return new NoopCommand();
    }

    @Bean
    public Command quitCommand() {
        return new QuitCommand();
    }

    @Bean
    public Command receiptCommand() {
        return withAuthCheckWhenRequired(withTlsCheckWhenRequired(new ReceiptCommand()));
    }

    @Bean
    public Command resetCommand() {
        return withTlsCheckWhenRequired(new ResetCommand());
    }

    @Bean
    public Command startTLSCommand() {
        return new StartTLSCommand();
    }

    @Bean
    public Command verifyCommand() {
        return withAuthCheckWhenRequired(withTlsCheckWhenRequired(new VerifyCommand()));
    }

    @Bean
    public Command expandCommand() {
        return withAuthCheckWhenRequired(withTlsCheckWhenRequired(new ExpandCommand()));
    }

    private Command withTlsCheckWhenRequired(Command c) {
        return new RequireTLSCommandWrapper(c);
    }

    private Command withAuthCheckWhenRequired(Command c) {
        return new RequireAuthCommandWrapper(c);
    }
}
