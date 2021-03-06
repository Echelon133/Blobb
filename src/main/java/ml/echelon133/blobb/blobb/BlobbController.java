package ml.echelon133.blobb.blobb;

import ml.echelon133.blobb.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/blobbs")
public class BlobbController {

    private IBlobbService blobbService;

    @Autowired
    public BlobbController(IBlobbService blobbService) {
        this.blobbService = blobbService;
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<FeedBlobb> getBlobbWithUuid(@PathVariable String uuid) throws Exception {
        return new ResponseEntity<>(
                blobbService.getByUuid(UUID.fromString(uuid)),
                HttpStatus.OK);
    }

    @GetMapping("/{uuid}/info")
    public ResponseEntity<BlobbInfo> getInfoAboutBlobbWithUuid(@PathVariable String uuid) throws Exception {
        return new ResponseEntity<>(
                blobbService.getBlobbInfo(UUID.fromString(uuid)),
                HttpStatus.OK);
    }


    @GetMapping("/{uuid}/responses")
    public ResponseEntity<List<FeedBlobb>> getResponsesToBlobb(@PathVariable String uuid,
                                                               @RequestParam(required = false) Long skip,
                                                               @RequestParam(required = false) Long limit) throws Exception {
        if (skip == null) {
            skip = 0L;
        }
        if (limit == null) {
            limit = 5L;
        }

        return new ResponseEntity<>(
                blobbService.getAllResponsesTo(UUID.fromString(uuid), skip, limit),
                HttpStatus.OK);
    }

    @GetMapping("/{uuid}/reblobbs")
    public ResponseEntity<List<FeedBlobb>> getReblobbsOfBlobb(@PathVariable String uuid,
                                                              @RequestParam(required = false) Long skip,
                                                              @RequestParam(required = false) Long limit) throws Exception {
        if (skip == null) {
            skip = 0L;
        }
        if (limit == null) {
            limit = 5L;
        }

        return new ResponseEntity<>(
                blobbService.getAllReblobbsOf(UUID.fromString(uuid), skip, limit),
                HttpStatus.OK);
    }

    @GetMapping("/{uuid}/like")
    public ResponseEntity<Map<String, Boolean>> checkIfLikes(@PathVariable String uuid) throws Exception {
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Boolean result = blobbService.checkIfUserWithUuidLikes(loggedUser, UUID.fromString(uuid));

        Map<String, Boolean> response = Map.of("liked", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{uuid}/like")
    public ResponseEntity<Map<String, Boolean>> likeBlobb(@PathVariable String uuid) throws Exception {
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Boolean result = blobbService.likeBlobb(loggedUser, UUID.fromString(uuid));

        Map<String, Boolean> response = Map.of("liked", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{uuid}/unlike")
    public ResponseEntity<Map<String, Boolean>> unlikeBlobb(@PathVariable String uuid) throws Exception {
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Boolean result = blobbService.unlikeBlobb(loggedUser, UUID.fromString(uuid));

        Map<String, Boolean> response = Map.of("unliked", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<Map<String, String>> postBlobb(@Valid @RequestBody BlobbDto blobbDto, BindingResult result) throws Exception {

        if (result.hasErrors()) {
            if (result.getFieldError() != null)
                throw new InvalidBlobbContentException(result.getFieldError().getDefaultMessage());
            else
                throw new InvalidBlobbContentException();
        }

        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Blobb savedBlobb = blobbService.postBlobb(loggedUser, blobbDto.getContent());

        return new ResponseEntity<>(
                Map.of("blobbUUID", savedBlobb.getUuid().toString(),
                       "content", savedBlobb.getContent(),
                       "author", loggedUser.getUsername()),
                HttpStatus.OK
        );
    }

    @PostMapping("/{uuid}/respond")
    public ResponseEntity<Map<String, String>> respondToBlobb(@PathVariable String uuid,
                                                              @Valid @RequestBody ResponseDto responseDto,
                                                              BindingResult result) throws Exception {

        if (result.hasErrors()) {
            if (result.getFieldError() != null)
                throw new InvalidBlobbContentException(result.getFieldError().getDefaultMessage());
            else
                throw new InvalidBlobbContentException();
        }

        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Blobb savedResponse = blobbService.postResponse(loggedUser, responseDto.getContent(), UUID.fromString(uuid));

        return new ResponseEntity<>(
                Map.of("blobbUUID", savedResponse.getUuid().toString(),
                       "content", savedResponse.getContent(),
                       "author", loggedUser.getUsername()),
                HttpStatus.OK
        );
    }

    @PostMapping("/{uuid}/reblobb")
    public ResponseEntity<Map<String, String>> reblobbOfBlobb(@PathVariable String uuid,
                                                              @Valid @RequestBody ReblobbDto reblobbDto,
                                                              BindingResult result) throws Exception {

        if (result.hasErrors()) {
            if (result.getFieldError() != null)
                throw new InvalidBlobbContentException(result.getFieldError().getDefaultMessage());
            else
                throw new InvalidBlobbContentException();
        }

        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Blobb savedReblobb = blobbService.postReblobb(loggedUser, reblobbDto.getContent(), UUID.fromString(uuid));

        return new ResponseEntity<>(
                Map.of("blobbUUID", savedReblobb.getUuid().toString(),
                       "content", savedReblobb.getContent(),
                       "author", loggedUser.getUsername()),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, Boolean>> deleteBlobb(@PathVariable String uuid) throws Exception {
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean deleted = blobbService.markBlobbAsDeleted(loggedUser, UUID.fromString(uuid));

        return new ResponseEntity<>(
                Map.of("deleted", deleted),
                HttpStatus.OK
        );
    }
}
