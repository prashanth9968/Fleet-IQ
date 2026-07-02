package com.fleetiq.vehicle.service;

import com.fleetiq.vehicle.config.TenantContext;
import com.fleetiq.vehicle.dto.request.AssignDeviceRequest;
import com.fleetiq.vehicle.dto.request.AssignDriverRequest;
import com.fleetiq.vehicle.dto.request.CreateVehicleRequest;
import com.fleetiq.vehicle.dto.response.VehicleResponse;
import com.fleetiq.vehicle.entity.*;
import com.fleetiq.vehicle.exception.BadRequestException;
import com.fleetiq.vehicle.exception.ResourceNotFoundException;
import com.fleetiq.vehicle.exception.UnauthorizedException;
import com.fleetiq.vehicle.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final DepotRepository depotRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DriverRepository driverRepository;
    private final DeviceVehicleAssignmentRepository deviceVehicleAssignmentRepository;
    private final DriverAssignmentRepository driverAssignmentRepository;

    public VehicleServiceImpl(
            VehicleRepository vehicleRepository,
            VehicleTypeRepository vehicleTypeRepository,
            DepotRepository depotRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            DriverRepository driverRepository,
            DeviceVehicleAssignmentRepository deviceVehicleAssignmentRepository,
            DriverAssignmentRepository driverAssignmentRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.depotRepository = depotRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.driverRepository = driverRepository;
        this.deviceVehicleAssignmentRepository = deviceVehicleAssignmentRepository;
        this.driverAssignmentRepository = driverAssignmentRepository;
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new UnauthorizedException("Tenant context (X-Tenant-ID) is missing or invalid");
        }
        return tenantId;
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        UUID tenantId = getRequiredTenantId();

        if (vehicleRepository.existsByRegistrationNumberAndTenantIdAndDeletedAtIsNull(request.registrationNumber(), tenantId)) {
            throw new BadRequestException("A vehicle with registration number " + request.registrationNumber() + " already exists for this tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        VehicleType vehicleType = vehicleTypeRepository.findById(UUID.fromString(request.vehicleTypeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle Type not found"));

        Depot depot = null;
        if (request.depotId() != null && !request.depotId().trim().isEmpty()) {
            depot = depotRepository.findByIdAndTenantIdAndDeletedAtIsNull(UUID.fromString(request.depotId()), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Depot not found for this tenant"));
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setTenant(tenant);
        vehicle.setVehicleType(vehicleType);
        vehicle.setDepot(depot);
        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setVin(request.vin());
        vehicle.setChassisNumber(request.chassisNumber());
        vehicle.setEngineNumber(request.engineNumber());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setYearOfManufacture(request.yearOfManufacture());
        vehicle.setColor(request.color());
        vehicle.setFuelTankCapacityLitres(request.fuelTankCapacityLitres());
        vehicle.setOdometerReadingKm(request.odometerReadingKm());
        vehicle.setEngineHours(request.engineHours());
        
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
        
        vehicle.setAcquisitionDate(request.acquisitionDate());
        vehicle.setAcquisitionCost(request.acquisitionCost());
        vehicle.setInsuranceExpiry(request.insuranceExpiry());
        vehicle.setPermitExpiry(request.permitExpiry());
        vehicle.setFitnessExpiry(request.fitnessExpiry());

        vehicle = vehicleRepository.save(vehicle);
        return mapToResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(UUID id) {
        UUID tenantId = getRequiredTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return mapToResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        UUID tenantId = getRequiredTenantId();
        return vehicleRepository.findAllByTenantIdAndDeletedAtIsNull(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID id, CreateVehicleRequest request) {
        UUID tenantId = getRequiredTenantId();

        Vehicle vehicle = vehicleRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!vehicle.getRegistrationNumber().equals(request.registrationNumber()) &&
                vehicleRepository.existsByRegistrationNumberAndTenantIdAndDeletedAtIsNull(request.registrationNumber(), tenantId)) {
            throw new BadRequestException("A vehicle with registration number " + request.registrationNumber() + " already exists for this tenant");
        }

        VehicleType vehicleType = vehicleTypeRepository.findById(UUID.fromString(request.vehicleTypeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle Type not found"));

        Depot depot = null;
        if (request.depotId() != null && !request.depotId().trim().isEmpty()) {
            depot = depotRepository.findByIdAndTenantIdAndDeletedAtIsNull(UUID.fromString(request.depotId()), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Depot not found for this tenant"));
        }

        vehicle.setVehicleType(vehicleType);
        vehicle.setDepot(depot);
        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setVin(request.vin());
        vehicle.setChassisNumber(request.chassisNumber());
        vehicle.setEngineNumber(request.engineNumber());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setYearOfManufacture(request.yearOfManufacture());
        vehicle.setColor(request.color());
        vehicle.setFuelTankCapacityLitres(request.fuelTankCapacityLitres());
        vehicle.setOdometerReadingKm(request.odometerReadingKm());
        vehicle.setEngineHours(request.engineHours());
        
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
        
        vehicle.setAcquisitionDate(request.acquisitionDate());
        vehicle.setAcquisitionCost(request.acquisitionCost());
        vehicle.setInsuranceExpiry(request.insuranceExpiry());
        vehicle.setPermitExpiry(request.permitExpiry());
        vehicle.setFitnessExpiry(request.fitnessExpiry());
        vehicle.setUpdatedAt(OffsetDateTime.now());

        vehicle = vehicleRepository.save(vehicle);
        return mapToResponse(vehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(UUID id) {
        UUID tenantId = getRequiredTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicle.setDeletedAt(OffsetDateTime.now());
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void assignDevice(UUID vehicleId, AssignDeviceRequest request, UUID assignedByUserId) {
        UUID tenantId = getRequiredTenantId();

        Vehicle vehicle = vehicleRepository.findByIdAndTenantIdAndDeletedAtIsNull(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Device device = deviceRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.deviceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        User user = userRepository.findById(assignedByUserId)
                .orElse(null);

        // Terminate active assignments for the device
        deviceVehicleAssignmentRepository.findByDeviceIdAndUnassignedAtIsNull(device.getId())
                .ifPresent(assignment -> {
                    assignment.setUnassignedAt(OffsetDateTime.now());
                    deviceVehicleAssignmentRepository.save(assignment);
                });

        // Terminate active assignments for the vehicle
        deviceVehicleAssignmentRepository.findByVehicleIdAndUnassignedAtIsNull(vehicle.getId())
                .ifPresent(assignment -> {
                    assignment.setUnassignedAt(OffsetDateTime.now());
                    deviceVehicleAssignmentRepository.save(assignment);
                });

        // Create new assignment
        DeviceVehicleAssignment assignment = new DeviceVehicleAssignment();
        assignment.setVehicle(vehicle);
        assignment.setDevice(device);
        assignment.setAssignedBy(user);
        assignment.setIsPrimary(request.isPrimary() == null || request.isPrimary());
        deviceVehicleAssignmentRepository.save(assignment);

        // Update device status to ACTIVE
        device.setStatus("ACTIVE");
        device.setUpdatedAt(OffsetDateTime.now());
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void assignDriver(UUID vehicleId, AssignDriverRequest request, UUID assignedByUserId) {
        UUID tenantId = getRequiredTenantId();

        Vehicle vehicle = vehicleRepository.findByIdAndTenantIdAndDeletedAtIsNull(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Driver driver = driverRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.driverId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        User user = userRepository.findById(assignedByUserId)
                .orElse(null);

        // Complete any active assignment for the driver
        driverAssignmentRepository.findByDriverIdAndStatus(driver.getId(), "ACTIVE")
                .ifPresent(assignment -> {
                    assignment.setStatus("COMPLETED");
                    assignment.setShiftEnd(OffsetDateTime.now());
                    assignment.setUpdatedAt(OffsetDateTime.now());
                    driverAssignmentRepository.save(assignment);
                });

        // Complete any active assignment for the vehicle
        driverAssignmentRepository.findByVehicleIdAndStatus(vehicle.getId(), "ACTIVE")
                .ifPresent(assignment -> {
                    assignment.setStatus("COMPLETED");
                    assignment.setShiftEnd(OffsetDateTime.now());
                    assignment.setUpdatedAt(OffsetDateTime.now());
                    driverAssignmentRepository.save(assignment);
                });

        // Create new assignment
        DriverAssignment assignment = new DriverAssignment();
        assignment.setTenant(vehicle.getTenant());
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setShiftStart(request.shiftStart());
        assignment.setShiftEnd(request.shiftEnd());
        assignment.setAssignedBy(user);
        assignment.setStatus("ACTIVE");
        assignment.setNotes(request.notes());
        driverAssignmentRepository.save(assignment);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        UUID activeDeviceId = null;
        String activeDeviceSerial = null;
        String activeDeviceType = null;
        
        UUID activeDriverId = null;
        String activeDriverName = null;

        DeviceVehicleAssignment devAssign = deviceVehicleAssignmentRepository
                .findByVehicleIdAndUnassignedAtIsNull(vehicle.getId()).orElse(null);
        if (devAssign != null) {
            activeDeviceId = devAssign.getDevice().getId();
            activeDeviceSerial = devAssign.getDevice().getSerialNumber();
            activeDeviceType = devAssign.getDevice().getDeviceType();
        }

        DriverAssignment drvAssign = driverAssignmentRepository
                .findByVehicleIdAndStatus(vehicle.getId(), "ACTIVE").orElse(null);
        if (drvAssign != null) {
            activeDriverId = drvAssign.getDriver().getId();
            activeDriverName = drvAssign.getDriver().getFirstName() + " " + drvAssign.getDriver().getLastName();
        }

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getTenant().getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getVin(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYearOfManufacture(),
                vehicle.getColor(),
                vehicle.getFuelTankCapacityLitres(),
                vehicle.getOdometerReadingKm(),
                vehicle.getEngineHours(),
                vehicle.getStatus(),
                vehicle.getAcquisitionDate(),
                vehicle.getVehicleType().getId(),
                vehicle.getVehicleType().getName(),
                vehicle.getVehicleType().getCategory(),
                vehicle.getVehicleType().getFuelType(),
                vehicle.getDepot() != null ? vehicle.getDepot().getId() : null,
                vehicle.getDepot() != null ? vehicle.getDepot().getName() : null,
                activeDeviceId,
                activeDeviceSerial,
                activeDeviceType,
                activeDriverId,
                activeDriverName,
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}
