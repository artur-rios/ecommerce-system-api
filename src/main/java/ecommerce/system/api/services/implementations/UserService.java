package ecommerce.system.api.services.implementations;

import ecommerce.system.api.enums.RolesEnum;
import ecommerce.system.api.exceptions.BatchUpdateException;
import ecommerce.system.api.exceptions.EmptySearchException;
import ecommerce.system.api.exceptions.ForbiddenException;
import ecommerce.system.api.models.UserModel;
import ecommerce.system.api.repositories.IUserRepository;
import ecommerce.system.api.services.IUserService;
import ecommerce.system.api.tools.SHAEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class UserService implements IUserService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final IUserRepository userRepository;
    private final SHAEncoder shaEncoder;

    @Value("${images.path.users.default}")
    private String defaultProfileImagePath;

    @Autowired
    public UserService(IUserRepository userRepository, SHAEncoder shaEncoder) {
        this.userRepository = userRepository;
        this.shaEncoder = shaEncoder;
    }

    @Override
    public void createUser(UserModel user) throws NoSuchAlgorithmException, EmptySearchException {

        String encodedPassword = this.shaEncoder.encode(user.getPassword());

        user.setPassword(encodedPassword);
        user.setCreationDate(LocalDateTime.now());
        user.setLastUpdate(null);
        user.setActive(true);

        if (user.getProfileImagePath() == null) {
            user.setProfileImagePath(this.defaultProfileImagePath);
        }

        if (this.userRepository.checkUserByEmail(user.getEmail(), false)) {
            this.updateUser(user);

        } else {
            this.userRepository.create(user);
        }
    }

    @Override
    public void createCustomer(UserModel user)
            throws ForbiddenException, NoSuchAlgorithmException, EmptySearchException {

        String userRole = RolesEnum.getRoleById(user.getRoleId());

        if(userRole != "customer") {
            throw new ForbiddenException("Operação não permitida!");
        }

        this.createUser(user);
    }

    @Override
    public ArrayList<UserModel> getAllUsers() throws EmptySearchException {

        return this.userRepository.getAll();
    }

    @Override
    public UserModel getUserById(int id) {

        return this.userRepository.getById(id);
    }

    @Override
    public UserModel getUserByEmail(String email) {

        return this.userRepository.getUserByEmail(email);
    }

    @Override
    public UserModel getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return this.getUserByEmail(email);
    }

    @Override
    public void updateUser(UserModel user) throws EmptySearchException {

        UserModel oldUser = this.userRepository.getById(user.getUserId());

        user.setPassword(oldUser.getPassword());
        user.setCreationDate(oldUser.getCreationDate());
        user.setActive(oldUser.isActive());
        user.setLastUpdate(LocalDateTime.now());

        this.userRepository.update(user);
    }

    @Override
    public void updateUserPassword(int userId, String email, String password, int roleId) throws ForbiddenException {

        String role = RolesEnum.getRoleById(roleId);
        UserModel user = this.userRepository.getById(userId);
        String loggedEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        if (role.equals("store_employee") || role.equals("customer")) {

            if(!(user.getEmail().equals(loggedEmail))) {
                throw new ForbiddenException("Operação não autorizada");
            }
        }

        // TO DO - Flux to store_admin role

        user.setPassword(password);
        user.setLastUpdate(LocalDateTime.now());

        this.userRepository.update(user);
    }

    @Override
    public void deleteUserProfile(int id) throws ForbiddenException, BatchUpdateException {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserModel user = this.userRepository.getUserByEmail(email);

        if (id != user.getUserId()) {
            throw new ForbiddenException("Operação não permitida!");
        }

        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(id);

        this.userRepository.delete(ids);
    }

    @Override
    public void deleteUsers(ArrayList<Integer> ids) throws BatchUpdateException {

        logger.info("Deleting " + ids.size() + " users...");

        (ids).forEach((id) -> {
            UserModel user = this.userRepository.getById(id);
            logger.info("User " + user.getUserId() + " found.");
        });

        this.userRepository.delete(ids);
    }
}
