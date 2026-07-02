package com.fleetiq.vehicle;

import com.fleetiq.vehicle.dto.request.AssignDeviceRequest;
import com.fleetiq.vehicle.dto.request.AssignDriverRequest;
import com.fleetiq.vehicle.dto.request.CreateVehicleRequest;
import com.fleetiq.vehicle.dto.response.VehicleResponse;
import com.fleetiq.vehicle.entity.*;
import com.fleetiq.vehicle.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class VehicleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleTypeRepository vehicleTypeRepository;

    @Autowired
    private DepotRepository depotRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DeviceVehicleAssignmentRepository deviceVehicleAssignmentRepository;

    @Autowired
    private DriverAssignmentRepository driverAssignmentRepository;

    private Tenant testTenant;
    private User testUser;
    private VehicleType testVehicleType;
    private Depot testDepot;
    private Device testDevice;
    private Driver testDriver;
    private String jwtToken;

    @BeforeEach
    public void setUp() {
        deviceVehicleAssignmentRepository.deleteAll();
        driverAssignmentRepository.deleteAll();
        vehicleRepository.deleteAll();
        deviceRepository.deleteAll();
        driverRepository.deleteAll();
        depotRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Create Tenant
        testTenant = new Tenant();
        testTenant.setName("Fleet Demo Corp");
        testTenant.setSlug("fleet-demo");
        testTenant.setStatus("ACTIVE");
        testTenant = tenantRepository.save(testTenant);

        // 2. Create User
        testUser = new User();
        testUser.setTenant(testTenant);
        testUser.setEmail("manager@fleetdemo.com");
        testUser.setFirstName("Sunita");
        testUser.setLastName("Patel");
        testUser.setStatus("ACTIVE");
        testUser = userRepository.save(testUser);

        // 3. Create Vehicle Type
        testVehicleType = new VehicleType();
        testVehicleType.setName("Medium Delivery Truck");
        testVehicleType.setCategory("TRUCK_MEDIUM");
        testVehicleType.setFuelType("DIESEL");
        testVehicleType.setDefaultFuelCapacityLitres(BigDecimal.valueOf(120.00));
        testVehicleType.setDefaultFuelConsumptionRate(BigDecimal.valueOf(14.50));
        testVehicleType = vehicleTypeRepository.save(testVehicleType);

        // 4. Create Depot
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = geometryFactory.createPoint(new Coordinate(78.486, 17.385));
        
        testDepot = new Depot();
        testDepot.setTenant(testTenant);
        testDepot.setName("Hyderabad South Hub");
        testDepot.setCode("HYD_SOUTH");
        testDepot.setLocation(point);
        testDepot.setCapacity(50);
        testDepot.setActive(true);
        testDepot = depotRepository.save(testDepot);

        // 5. Create Device
        testDevice = new Device();
        testDevice.setTenant(testTenant);
        testDevice.setSerialNumber("SN-OBD-98765");
        testDevice.setImei("358976543210987");
        testDevice.setDeviceType("OBD_DONGLE");
        testDevice.setManufacturer("Teltonika");
        testDevice.setModel("FMB001");
        testDevice.setStatus("INACTIVE");
        testDevice = deviceRepository.save(testDevice);

        // 6. Create Driver
        testDriver = new Driver();
        testDriver.setTenant(testTenant);
        testDriver.setFirstName("Amit");
        testDriver.setLastName("Kumar");
        testDriver.setPhone("+919999999999");
        testDriver.setEmployeeId("EMP-1002");
        testDriver.setStatus("ACTIVE");
        testDriver = driverRepository.save(testDriver);

        // 7. Generate JWT
        jwtToken = generateTestToken("manager@fleetdemo.com", testTenant.getId(), "FLEET_MANAGER", 
                List.of("vehicles:create", "vehicles:view", "vehicles:update", "vehicles:delete", "drivers:assign"));
    }

    private String generateTestToken(String email, UUID tenantId, String role, List<String> permissions) {
        byte[] keyBytes = "4z3W2q1p0o9n8m7l6k5j4i3h2g1f0e9d8c7b6a5y4x3w2u1t0s9r8q7p6o5n4m3l2k1j0".getBytes(StandardCharsets.UTF_8);
        SecretKey signingKey = Keys.hmacShaKeyFor(keyBytes);

        Map<String, Object> extraClaims = new HashMap<>();
        if (tenantId != null) {
            extraClaims.put("tenant_id", tenantId.toString());
        }
        extraClaims.put("role", role);
        extraClaims.put("permissions", permissions);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.set("X-Tenant-ID", testTenant.getId().toString());
        return headers;
    }

    @Test
    public void testVehicleLifecycleAndAssignments() {
        HttpHeaders headers = createAuthHeaders();

        // Step 1: Create Vehicle
        CreateVehicleRequest createRequest = new CreateVehicleRequest(
                "TS-09-UB-4321",
                testVehicleType.getId().toString(),
                testDepot.getId().toString(),
                "1234567890ABCDEFG",
                "CHASSIS-999",
                "ENGINE-888",
                "Tata Motors",
                "Ultra T.7",
                2024,
                "White",
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(2500.00),
                BigDecimal.valueOf(150.00),
                "ACTIVE",
                LocalDate.now().minusMonths(6),
                BigDecimal.valueOf(1500000.00),
                LocalDate.now().plusMonths(6),
                LocalDate.now().plusMonths(12),
                LocalDate.now().plusMonths(24)
        );

        HttpEntity<CreateVehicleRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<VehicleResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/vehicles",
                createEntity,
                VehicleResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        UUID vehicleId = createResponse.getBody().id();
        assertThat(createResponse.getBody().registrationNumber()).isEqualTo("TS-09-UB-4321");
        assertThat(createResponse.getBody().depotName()).isEqualTo("Hyderabad South Hub");

        // Step 2: Get Vehicle
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<VehicleResponse> getResponse = restTemplate.exchange(
                "/api/v1/vehicles/" + vehicleId,
                HttpMethod.GET,
                getEntity,
                VehicleResponse.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().registrationNumber()).isEqualTo("TS-09-UB-4321");

        // Step 3: Assign Device
        AssignDeviceRequest deviceRequest = new AssignDeviceRequest(testDevice.getId(), true);
        HttpEntity<AssignDeviceRequest> deviceEntity = new HttpEntity<>(deviceRequest, headers);
        ResponseEntity<Void> deviceResponse = restTemplate.postForEntity(
                "/api/v1/vehicles/" + vehicleId + "/assign-device",
                deviceEntity,
                Void.class
        );
        assertThat(deviceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 4: Assign Driver
        AssignDriverRequest driverRequest = new AssignDriverRequest(
                testDriver.getId(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(8),
                "Standard Day Shift"
        );
        HttpEntity<AssignDriverRequest> driverEntity = new HttpEntity<>(driverRequest, headers);
        ResponseEntity<Void> driverResponse = restTemplate.postForEntity(
                "/api/v1/vehicles/" + vehicleId + "/assign-driver",
                driverEntity,
                Void.class
        );
        assertThat(driverResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 5: Get Vehicle again and check assignments are populated
        ResponseEntity<VehicleResponse> getResponseAfterAssign = restTemplate.exchange(
                "/api/v1/vehicles/" + vehicleId,
                HttpMethod.GET,
                getEntity,
                VehicleResponse.class
        );
        assertThat(getResponseAfterAssign.getBody().activeDeviceSerial()).isEqualTo("SN-OBD-98765");
        assertThat(getResponseAfterAssign.getBody().activeDriverName()).isEqualTo("Amit Kumar");

        // Step 6: Delete Vehicle
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/vehicles/" + vehicleId,
                HttpMethod.DELETE,
                getEntity,
                Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Step 7: Verify it is no longer retrievable (should return 404)
        ResponseEntity<ProblemDetail> getResponseAfterDelete = restTemplate.exchange(
                "/api/v1/vehicles/" + vehicleId,
                HttpMethod.GET,
                getEntity,
                ProblemDetail.class
        );
        assertThat(getResponseAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
