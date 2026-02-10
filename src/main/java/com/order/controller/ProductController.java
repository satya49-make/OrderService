package com.order.controller;

import com.order.dto.ApiResponse;
import com.order.dto.ProductDTO;
import com.order.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@Slf4j
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Get product by ID
     * GET /api/products/{productId}
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long productId) {
        log.info("Fetching product with ID: {}", productId);
        ProductDTO productDTO = productService.getProductById(productId);
        ApiResponse<ProductDTO> response = new ApiResponse<>(true, "Product retrieved successfully", productDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get all available products
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllAvailableProducts() {
        log.info("Fetching all available products");
        List<ProductDTO> products = productService.getAllAvailableProducts();
        ApiResponse<List<ProductDTO>> response = new ApiResponse<>(true, "Products retrieved successfully", products);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Search products by name
     * GET /api/products/search?name={productName}
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> searchProducts(@RequestParam String name) {
        log.info("Searching products with name: {}", name);
        List<ProductDTO> products = productService.searchProductsByName(name);
        ApiResponse<List<ProductDTO>> response = new ApiResponse<>(true, "Products found", products);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Create a new product
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@RequestBody ProductDTO productDTO) {
        log.info("Creating new product: {}", productDTO.getProductName());
        ProductDTO createdProduct = productService.createProduct(productDTO);
        ApiResponse<ProductDTO> response = new ApiResponse<>(true, "Product created successfully", createdProduct);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update product
     * PUT /api/products/{productId}
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductDTO productDTO) {
        log.info("Updating product with ID: {}", productId);
        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO);
        ApiResponse<ProductDTO> response = new ApiResponse<>(true, "Product updated successfully", updatedProduct);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

