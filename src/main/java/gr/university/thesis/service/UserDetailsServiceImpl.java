package gr.university.thesis.service;


import gr.university.thesis.entity.Role;
import gr.university.thesis.entity.SessionUser;
import gr.university.thesis.entity.User;
import gr.university.thesis.repository.RoleRepository;
import gr.university.thesis.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * Class that manages current user details loading
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    //avoiding magic numbers here
    static final String ADMIN_EMAIL = "mcjohn1@windowslive.com";
    static final String ADMIN_PASSWORD = "321";
    UserRepository userRepository;
    RoleRepository roleRepository;

    /**
     * constructor of this class, correct way to set the autowired attributes
     *
     * @param userRepository: repository that has access to all the users of the system
     * @param roleRepository  : repository that has access to all the roles of the system
     */
    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * searches for user in the repository
     *
     * @param email takes as input an email, which is unique to every user
     * @return returns a User if he is found in the repository
     * @throws UsernameNotFoundException throws this exception if user is not found in the repository
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        //these attributes are required by the Spring User Details class
        boolean enabled = true;
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = true;
        //Search for the user within the repository, and if the user doesn't exist, throw an exception
        User repoUser =
                userRepository.findFirstByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User does not exist"));
        //Map the authority list with the spring security list
        List grantList = new ArrayList();
        for (Role role : repoUser.getRoles()) {
            // ROLE:USER or ROLE:ADMIN or BOTH
            GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(role.getRole());
            grantList.add(grantedAuthority);
        }
        SessionUser user = new SessionUser(repoUser.getId(), repoUser.getEmail(), repoUser.getPassword(), enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, grantList);
        return user;
    }

    /**
     * initializes some essential data that the system requires in order to work properly
     * creates roles(ADMIN/USER) and sets a master admin
     */
    public void firstTime() {
        if (!userRepository.findFirstByEmail(ADMIN_EMAIL).isPresent()) {
            User user = new User();
            user.setEmail(ADMIN_EMAIL);
            user.setFirstName("John");
            user.setLastName("Daniel");
            //encrypt password
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(4);
            String encodedPassword = bCryptPasswordEncoder.encode(ADMIN_PASSWORD);
            user.setPassword(encodedPassword);
            //create role for user
            List<Role> allRoles = new ArrayList<>();
            Role masterRole = new Role();
            masterRole.setId((long) 1);
            masterRole.setRole("MASTER_ADMIN");
            allRoles.add(masterRole);
            Role adminRole = new Role();
            adminRole.setId((long) 2);
            adminRole.setRole("ADMIN");
            allRoles.add(adminRole);
            Role projectManagerRole = new Role();
            projectManagerRole.setId((long) 3);
            projectManagerRole.setRole("PRODUCT_OWNER");
            allRoles.add(projectManagerRole);
            Role userRole = new Role();
            userRole.setId((long) 4);
            userRole.setRole("DEVELOPER");
            allRoles.add(userRole);
            roleRepository.saveAll(allRoles);
            user.setRoles(allRoles);
            userRepository.save(user);
        }
    }
}
