package com.order.service;

import com.order.dto.ProductDTO;
import com.order.entity.Product;
import com.order.entity.ProductStatus;
import com.order.exception.ProductNotAvailableException;
import com.order.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Get product by ID
     */
    public ProductDTO getProductById(Long productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotAvailableException("Product not found with ID: " + productId));
        return convertToDTO(product);
    }

    /**
     * Get all available products
     */
    public List<ProductDTO> getAllAvailableProducts() {
        List<Product> products = productRepository.findByStatus(ProductStatus.AVAILABLE);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search products by name
     */
    public List<ProductDTO> searchProductsByName(String productName) {
        List<Product> products = productRepository.findByProductNameContainingIgnoreCase(productName);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new product
     */
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setProductName(productDTO.getProductName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setAvailableQuantity(productDTO.getAvailableQuantity());
        product.setStatus(ProductStatus.AVAILABLE);

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getProductId());
        return convertToDTO(savedProduct);
    }

    /**
     * Update product
     */
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotAvailableException("Product not found with ID: " + productId));

        if (productDTO.getProductName() != null) {
            product.setProductName(productDTO.getProductName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getAvailableQuantity() != null) {
            product.setAvailableQuantity(productDTO.getAvailableQuantity());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", productId);
        return convertToDTO(updatedProduct);
    }

    /**
     * Convert Product entity to ProductDTO
     */
    private ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
                product.getProductId(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice(),
                product.getAvailableQuantity(),
                product.getStatus().toString()
        );
    }
}

