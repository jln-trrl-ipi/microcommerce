package com.ecommerce.microcommande.microcommerce.web.controller;

import com.ecommerce.microcommande.microcommerce.web.exceptions.ProduitGratuitException;
import com.ecommerce.microcommande.microcommerce.web.exceptions.ProduitIntrouvableException;
import com.ecommerce.microcommande.microcommerce.dao.ProductDao;
import com.ecommerce.microcommande.microcommerce.model.Product;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.util.*;

import java.util.List;

@Api(description="API pour opérations de CRUD sur des produits.")
@RestController
public class ProductController {

    private static final Logger logger =
            LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductDao productDao;

    @Autowired
    private HttpServletRequest requestContext ;

    //Récupérer la liste des produits
    @ApiOperation(value="Récupère la liste des produits")
    @RequestMapping(value = "/Produits", method = RequestMethod.GET)
    public MappingJacksonValue listeProduits() {

        logger.info("Début d'appel au service Produit pour la requête : " +
                requestContext.getHeader("req-id"));
        Iterable<Product> produits = productDao.findAll();
        SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");
        FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
        MappingJacksonValue produitsFiltres = new MappingJacksonValue(produits);
        produitsFiltres.setFilters(listDeNosFiltres);
        return produitsFiltres;
    }


    //Récupérer un produit par son Id
    @ApiOperation(value="Affiche le produit dont l'id est passé en paramètre")
    @GetMapping(value = "/Produits/{id}")
    public Product afficherUnProduit(@PathVariable int id) {
        Product produit = productDao.findById(id);
        if (produit == null) {
            throw new ProduitIntrouvableException("Le produit avec l'id " + id + " est introuvable.");
        }
        return produit;
    }

    @ApiOperation(value="Affiche une map avec les produits et leur marge")
    @GetMapping(value = "/AdminProduits")
    public HashMap calculerMargeProduit() {
        List<Product> produits = productDao.findAll();
        HashMap response = new HashMap<String,Integer>();
        for(Product produit : produits){
            response.put(produit.toString(),produit.getPrix() - produit.getPrixAchat());
        }
        return response;
    }

    //Récupérer la liste des produits ordonnées par ordre alphabétique
    @ApiOperation(value="Récupère la liste des produits triés par ordre alphabétique")
    @RequestMapping(value = "/TriProduits", method = RequestMethod.GET)
    public MappingJacksonValue trierProduitsParOrdreAlphabetique() {
        Iterable<Product> produits = productDao.findAllByOrderByNomAsc();
        SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");
        FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
        MappingJacksonValue produitsFiltres = new MappingJacksonValue(produits);
        produitsFiltres.setFilters(listDeNosFiltres);
        return produitsFiltres;
    }

//    @GetMapping(value = "/test/produits/{prixLimit}")
//    public List<Product> testDeRequetes(@PathVariable int prixLimit) {
//        return productDao.findByPrixGreaterThan(prixLimit);
//    }

    @GetMapping(value = "/test/produits/{recherche}")
    public List<Product> testDeRequetes(@PathVariable String recherche) {
        return productDao.findByNomLike("%"+recherche+"%");
    }

    //ajouter un produit
    @ApiOperation(value="Ajoute un produit à partir d'un objet JSON passé dans le body.")
    @PostMapping(value = "/Produits")
    public ResponseEntity<Void> ajouterProduit(@Valid @RequestBody Product product) {
        if(product.getPrix() == 0){
            throw new ProduitGratuitException("Il n'est pas possible de créer un produit gratuit.");
        }
        Product productAdded = productDao.save(product);
        if (productAdded == null)
            return ResponseEntity.noContent().build();
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productAdded.getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @ApiOperation(value="Supprime le produit dont l'id est passé en paramètre")
    @DeleteMapping (value = "/Produits/{id}")
    public void supprimerProduit(@PathVariable int id) {
        productDao.delete(productDao.findById(id));
    }

    @ApiOperation(value="Met à jour le produit correspondant à l'id présent dans l'objet JSON passé en paramètre")
    @PutMapping (value = "/Produits")
    public void updateProduit(@RequestBody Product product) {
        productDao.save(product);
    }

}
