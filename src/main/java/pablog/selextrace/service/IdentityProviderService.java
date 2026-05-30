package pablog.selextrace.service;

import org.springframework.stereotype.Service;
import pablog.selextrace.model.auth.IdentityProvider;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.repository.GoogleIdentityRepository;
import pablog.selextrace.repository.PasswordIdentityRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class IdentityProviderService {

    private final PasswordIdentityRepository passwordIdentityRepository;
    private final GoogleIdentityRepository googleIdentityRepository;

    public IdentityProviderService(
            PasswordIdentityRepository passwordIdentityRepository,
            GoogleIdentityRepository googleIdentityRepository
    ) {
        this.passwordIdentityRepository = passwordIdentityRepository;
        this.googleIdentityRepository = googleIdentityRepository;
    }

    public List<IdentityProvider> linkedProviders(AppUserRecord user) {
        List<IdentityProvider> providers = new ArrayList<>();
        if (passwordIdentityRepository.existsByUser_Id(user.getId())) {
            providers.add(IdentityProvider.PASSWORD);
        }
        if (googleIdentityRepository.existsByUser_Id(user.getId())) {
            providers.add(IdentityProvider.GOOGLE);
        }
        return List.copyOf(providers);
    }
}