package ecommerce.system.api.repositories.implementations;

import ecommerce.system.api.entities.ProductTypeEntity;
import ecommerce.system.api.models.ProductTypeModel;
import ecommerce.system.api.repositories.IProductTypeRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(rollbackOn = {Exception.class})
public class ProductTypeRepository implements IProductTypeRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public int create(ProductTypeModel object) {

        ProductTypeEntity productType = new ProductTypeEntity(object);

        this.entityManager.persist(productType);
        this.entityManager.flush();

        return productType.getProductTypeId();
    }

    @Override
    public List<ProductTypeModel> getAll() {

        String query = "FROM ProductTypeEntity p ORDER BY p.productTypeId ASC";
        TypedQuery<ProductTypeEntity> result = this.entityManager.createQuery(query, ProductTypeEntity.class);
        List<ProductTypeEntity> entities = result.getResultList();

        if (entities == null || entities.isEmpty()) {
            return null;
        }

        ArrayList<ProductTypeModel> productTypes = new ArrayList<>();
        (entities).forEach(productType -> productTypes.add(productType.toModel()));

        return productTypes;
    }

    @Override
    public ProductTypeModel getById(int id) {

        ProductTypeEntity productType = this.entityManager.find(ProductTypeEntity.class, id);

        return productType == null ? null : productType.toModel();
    }

    @Override
    public boolean update(ProductTypeModel object) {

        ProductTypeEntity updatedProductType = new ProductTypeEntity(object);

        this.entityManager.merge(updatedProductType);

        return false;
    }

    @Override
    public boolean delete(int id) {

        ProductTypeEntity productType = this.entityManager.find(ProductTypeEntity.class, id);

        if (productType == null) {
            return false;
        }

        return true;
    }
}
