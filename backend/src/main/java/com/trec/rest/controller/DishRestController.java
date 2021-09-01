package com.trec.rest.controller;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trec.controller.DefaultModeAttributes;
import com.trec.model.Dish;
import com.trec.service.DishService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

//import es.codeurjc.board.Post;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dishes")
public class DishRestController extends DefaultModeAttributes{
	
	@Autowired
	private DishService dishService;

	@Operation(summary = "Get all dishes")
	@ApiResponses(value = {
			 @ApiResponse(
			 responseCode = "200",
			 description = "Found all dishes",
			 content = {@Content(
			 mediaType = "application/json",
			 schema = @Schema(implementation=Dish.class)
			 )}
			 ),
			 @ApiResponse(
			 responseCode = "404",
			 description = "Dishes not found",
			 content = @Content
			 )
			})

	@GetMapping("/") // Show all dishes
	public ResponseEntity<List<Dish>> findDishes() {
		return ResponseEntity.ok(dishService.findAll());
	}
	
	@Operation(summary = "Get a dish by its id")
		@ApiResponses(value = {
			 @ApiResponse(
			 responseCode = "200",
			 description = "Found the dish",
			 content = {@Content(
			 mediaType = "application/json",
			 schema = @Schema(implementation=Dish.class)
			 )}
			 ),
			 @ApiResponse(
			 responseCode = "400",
			 description = "Invalid id supplied",
			 content = @Content
			 ), 
			 @ApiResponse(
			 responseCode = "404",
			 description = "Dish not found",
			 content = @Content
			 )
			})
	
	@GetMapping("/{id}") // Show a dish
	public ResponseEntity<Dish> findDishById(@Parameter(description="id of dish to be searched") @PathVariable long id) {

		Optional<Dish> dish = dishService.findById(id);
		
		if (dish.isPresent()) {
			return ResponseEntity.ok(dish.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	@Operation(summary = "Get all dishes from a category")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "200",
		 description = "Found the dishes from a category",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		 responseCode = "400",
		 description = "Invalid category supplied",
		 content = @Content
		 ), 
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dishes from category not found",
		 content = @Content
		 )
		})
	
	@GetMapping("/category") // "/type?category=Desayuno"
	public ResponseEntity<List<Dish>> findDishesByTipe(@Parameter(description="category of dish to be searched (desayuno, comida or cena)") @RequestParam String category) {
		return ResponseEntity.ok(dishService.getByCategory(category));
	}
	
	@Operation(summary = "Delete a dish by its id (only admin can do that)")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "200",
		 description = "The dish has been deleted correctly",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		 responseCode = "400",
		 description = "Invalid id supplied",
		 content = @Content
		 ), 
		 @ApiResponse(
		responseCode = "403",
		description = "Forbidden. You have to be an admin to do this",
		content = @Content
		), 
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dish couldn't be deleted",
		 content = @Content
		 ),		 
		})
	
	@DeleteMapping("/{id}") //Delete Dish form database and all purchases
	public ResponseEntity<Dish> removeDish(@Parameter(description="id of the dish to be deleted") @PathVariable long id) {
		
		Optional<Dish> dish = dishService.findById(id);
		boolean ok = dishService.deleteDish(dish, id);
		
		if (ok){
			return ResponseEntity.ok(dish.get());
		} else {
			return ResponseEntity.notFound().build();
		}
		
	}
	
	@Operation(summary = "Upload a new dish without an image (only admin can do that)")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "201",
		 description = "The dish has been uploaded correctly",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		 responseCode = "400",
		 description = "Invalid form of introducing the data for the new dish",
		 content = @Content
		 ), 
		 @ApiResponse(
		responseCode = "403",
		description = "Forbidden. You have to be an admin to do this",
		content = @Content
		), 
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dish couldn't be created",
		 content = @Content
		 )
		})
	
	@PostMapping("/")//Create Dish without image
	public ResponseEntity<Dish> newDishProcess(@Parameter(description="to create a new dish: name, price, category and list of ingredients in JSON") @RequestBody Dish dish)  {

		dish.setImage(false);
		
		dishService.save(dish);
		
		URI location = fromCurrentRequest().path("/{id}")
				.buildAndExpand(dish.getId()).toUri();

		return ResponseEntity.created(location).body(dish);
	}

	@Operation(summary = "Update a dish not including the image (only admin can do that)")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "200",
		 description = "The dish has been updated correctly",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		 responseCode = "400",
		 description = "Invalid form of introducing the data for updating the dish",
		 content = @Content
		 ), 
		 @ApiResponse(
		responseCode = "403",
		description = "Forbidden. You have to be an admin to do this",
		content = @Content
		), 
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dish couldn't be updated",
		 content = @Content
		 )
		})
	
	@PutMapping("/{id}")//Change some fields from dish (not image)
	public ResponseEntity<Dish> replaceDish(@Parameter(description="id of dish to be updated")@PathVariable long id, @Parameter(description="information of the dish you need to change")@RequestBody Dish dish)
			throws IOException, SQLException {
		
		if (dish != null) {
			
			Dish oldish = dishService.findById(id).get();
			
			Dish newdish = dishService.updateDish(oldish,dish);
			
			dishService.save(newdish);

			return ResponseEntity.ok(newdish);

		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	@Operation(summary = "Upload the image of a dish (only admin can do that)")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "200",
		 description = "The dish image has been uploaded correctly",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		 responseCode = "400",
		 description = "Invalid form of introducing the image of the dish",
		 content = @Content
		 ), 
		 @ApiResponse(
		responseCode = "403",
		description = "Forbidden. You have to be an admin to do this",
		content = @Content
		), 
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dish image couldn't be uploaded",
		 content = @Content
		 )
		})
		
	@PostMapping("/{id}/image")
	public ResponseEntity<Object> uploadImage(@PathVariable long id, @RequestParam MultipartFile imageFile)
			throws IOException {

		Dish dish = dishService.findById(id).orElseThrow();

		URI location = fromCurrentRequest().build().toUri();

		dish.setImage(true);
		dish.setImageFile(BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
		dishService.save(dish);

		return ResponseEntity.created(location).build();
	}
	
	@Operation(summary = "Download the image of a dish")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "200",
		 description = "The dish image has been downloaded correctly",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dish image couldn't be downloaded",
		 content = @Content
		 )
		})
	
	@GetMapping("/{id}/image")
	public ResponseEntity<Object> downloadImage(@PathVariable long id) throws SQLException {

		Dish dish = dishService.findById(id).orElseThrow();

		if (dish.getImageFile() != null) {

			Resource file = new InputStreamResource(dish.getImageFile().getBinaryStream());

			return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
					.contentLength(dish.getImageFile().length()).body(file);

		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	@Operation(summary = "Delete the image of a dish (only admin can do that)")
	@ApiResponses(value = {
		 @ApiResponse(
		 responseCode = "204",
		 description = "No content. The dish image has been deleted correctly",
		 content = {@Content(
		 mediaType = "application/json",
		 schema = @Schema(implementation=Dish.class)
		 )}
		 ),
		 @ApiResponse(
		responseCode = "400",
	    description = "Invalid id supplied",
		content = @Content
		), 
		@ApiResponse(
	    responseCode = "403",
		description = "Forbidden. You have to be an admin to do this",
		content = @Content
		), 
		 @ApiResponse(
		 responseCode = "404",
		 description = "Dish image couldn't be deleted",
		 content = @Content
		 )
		})
	
	
	@DeleteMapping("/{id}/image")
	public ResponseEntity<Object> deleteImage(@PathVariable long id) throws IOException {

		Dish dish = dishService.findById(id).orElseThrow();

		dish.setImageFile(null);
		dish.setImage(false);

		dishService.save(dish);

		return ResponseEntity.noContent().build();
	}
}