package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CustomProductRepositoryImpl implements CustomProductRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Product> findByFilters(String category, String subcategory, String brand, Double minPrice, Double maxPrice, int page, int size) {
        // Create a Query object
        Query query = new Query();

        // Create a Criteria object to build the query conditions
        Criteria criteria = new Criteria();

        // Add filters only if the parameters are not null
        if (category != null) {
            criteria.and("category").is(category);
        }
        if (subcategory != null) {
            criteria.and("subcategory").is(subcategory);
        }
        if (brand != null) {
            criteria.and("brand").is(brand);
        }
        if (minPrice != null && maxPrice != null) {
            criteria.and("price").gte(minPrice).lte(maxPrice);
        } else if (minPrice != null) {
            criteria.and("price").gte(minPrice);
        } else if (maxPrice != null) {
            criteria.and("price").lte(maxPrice);
        }

        // Add the criteria to the query
        query.addCriteria(criteria);

        // Apply pagination
        query.skip((long) page * size).limit(size);

        // Execute the query and return the results
        return mongoTemplate.find(query, Product.class);
    }
}
