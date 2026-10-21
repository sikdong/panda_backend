package panda.access;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/map-access")
public class MapAccessController {

    private final MapAccessService mapAccessService;

    public MapAccessController(MapAccessService mapAccessService) {
        this.mapAccessService = mapAccessService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> grantAccess(@RequestBody Map<String, String> request) {
        try {
            String token = mapAccessService.issueToken(request.getOrDefault("password", ""));
            return ResponseEntity.ok(Map.of("token", token));
        } catch (MapAccessService.InvalidMapAccessPasswordException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "비밀번호를 확인해 주세요."));
        } catch (MapAccessService.MapAccessNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "지도 매물 비밀번호가 설정되지 않았습니다."));
        }
    }

    @GetMapping("/session")
    public Map<String, Boolean> checkSession(
            @RequestHeader(value = "X-Map-Access-Token", required = false) String token
    ) {
        return Map.of("authorized", mapAccessService.isAuthorized(token));
    }
}
