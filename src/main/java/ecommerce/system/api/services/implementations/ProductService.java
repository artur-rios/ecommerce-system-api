package ecommerce.system.api.services.implementations;

import ecommerce.system.api.enums.MessagesEnum;
import ecommerce.system.api.enums.OrderStatusEnum;
import ecommerce.system.api.exceptions.InvalidOperationException;
import ecommerce.system.api.models.*;
import ecommerce.system.api.repositories.IProductRepository;
import ecommerce.system.api.repositories.IProductSubtypeRepository;
import ecommerce.system.api.repositories.IProductTypeRepository;
import ecommerce.system.api.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService implements IProductService {

    private final IAuthenticationService authenticationService;
    private final IFileService fileService;
    private final IOrderService orderService;
    private final IProductRepository productRepository;
    private final IProductTypeRepository productTypeRepository;
    private final IProductSubtypeRepository productSubtypeRepository;
    private final IStoreService storeService;
    private final IUserService userService;

    @Autowired
    public ProductService(
            IAuthenticationService authenticationService,
            IFileService fileService,
            @Lazy IOrderService orderService,
            IProductRepository productRepository,
            IProductTypeRepository productTypeRepository,
            IProductSubtypeRepository productSubtypeRepository,
            IStoreService storeService,
            IUserService userService) {
        this.authenticationService = authenticationService;
        this.fileService = fileService;
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.productSubtypeRepository = productSubtypeRepository;
        this.storeService = storeService;
        this.userService = userService;
    }

    @Override
    public void createProduct(ProductModel product) {

        product.setCreationDate(LocalDateTime.now());
        product.setLastUpdate(null);
        product.setActive(true);

        this.productRepository.createProduct(product);
    }

    @Override
    public void createProductImage(MultipartFile file, int productId) throws InvalidOperationException, IOException {

        String imagePath = this.fileService.saveMultpartImage(file, "product", productId);

        ProductModel product = this.productRepository.getProductById(productId);

        if (product == null) {
            throw new InvalidOperationException("Produto não encontrado!");
        }

        this.productRepository.createProductImage(productId, imagePath);
    }

    @Override
    public List<ProductModel> getProductsByQuantity(int quantity) throws IOException {

        List<ProductModel> products = this.productRepository.getProductsByQuantity(quantity);

        if (products != null) {
            for (ProductModel product : products) {
                List<String> images = this.getImagesByPaths(product.getImageList());
                product.setImageList(images);
            }
        }

        return products;
    }

    @Override
    public List<ProductModel> getProductsByNameAndQuantity(String name, int quantity) throws IOException {

        List<ProductModel> products = this.productRepository.getProductsByNameAndQuantity(name, quantity);

        if (products != null) {
            for (ProductModel product : products) {
                List<String> images = this.getImagesByPaths(product.getImageList());
                product.setImageList(images);
            }
        }

        return products;
    }

    @Override
    public List<ProductModel> getProductsByStoreIdAndQuantity(int storeId, int quantity) throws IOException {

        List<ProductModel> products = this.productRepository.getProductsByStoreIdAndQuantity(storeId, quantity);

        if (products != null) {
            for (ProductModel product : products) {
                List<String> images = this.getImagesByPaths(product.getImageList());
                product.setImageList(images);
            }
        }

        return products;
    }

    @Override
    public List<ProductModel> getProductsBySubtypeIdAndQuantity(int subtypeId, int quantity) throws IOException {

        List<ProductModel> products = this.productRepository.getProductsBySubtypeIdAndQuantity(subtypeId, quantity);

        if (products != null) {
            for (ProductModel product : products) {
                List<String> images = this.getImagesByPaths(product.getImageList());
                product.setImageList(images);
            }
        }

        return products;
    }

    @Override
    public ProductModel getProductById(int productId) throws IOException {

        ProductModel product = this.productRepository.getProductById(productId);

        if (product != null) {
            List<String> images = this.getImagesByPaths(product.getImageList());
            product.setImageList(images);
        }

        return product;
    }

    @Override
    public List<ProductTypeModel> getAllProductTypes() {

        return this.productTypeRepository.getAll();
    }

    @Override
    public List<ProductSubtypeModel> getProductSubtypesByProductTypeId(int productTypeId) {

        return this.productSubtypeRepository.getByProductTypeId(productTypeId);
    }

    @Override
    public void updateProduct(ProductModel product) throws InvalidOperationException, IOException {

        ProductModel oldProduct = this.productRepository.getProductById(product.getProductId());

        if (oldProduct == null) {
            throw new InvalidOperationException("Produto não encontrado!");
        }

        StoreModel store = this.storeService.getStoreByProductId(product.getProductId());

        List<UserModel> users = this.userService.getUsersByStoreId(store.getStoreId());

        if (users.stream().noneMatch(user -> this.authenticationService.isLoggedUser(user.getUserId()))) {
            throw new InvalidOperationException(MessagesEnum.UNALLOWED.getMessage());
        }

        product.setCreationDate(oldProduct.getCreationDate());
        product.setLastUpdate(LocalDateTime.now());
        product.setActive(oldProduct.isActive());

        this.productRepository.updateProduct(product);
    }

    @Override
    public void deleteProduct(int productId) throws InvalidOperationException, IOException {

        ProductModel product = this.getProductById(productId);

        if (product == null) {
            throw new InvalidOperationException("Produto não encontrado.");
        }

        StoreModel store = this.storeService.getStoreByProductId(productId);

        List<UserModel> users = this.userService.getUsersByStoreId(store.getStoreId());

        if (users.stream().noneMatch(user -> this.authenticationService.isLoggedUser(user.getUserId()))) {
            throw new InvalidOperationException(MessagesEnum.UNALLOWED.getMessage());
        }

        List<OrderModel> orders = this.orderService.getOrdersByProductId(productId);

        for (OrderModel order : orders) {

            if (order.getOrderStatusId() != OrderStatusEnum.FINISHED.getId()) {
                throw new InvalidOperationException("Não é possível desativar um produto com pedidos em aberto. Caso queira removê-lo da loja antes, mude o quantidade em estoque para zero.");
            }
        }

        this.productRepository.deleteProduct(productId);
    }

    private List<String> getImagesByPaths(List<String> paths) throws IOException {

        ArrayList<String> images = new ArrayList<>();

        for (String path : paths) {
            images.add("data:image;base64, " + this.fileService.getImageBase64(path));
        }

        return images;
    }
}
