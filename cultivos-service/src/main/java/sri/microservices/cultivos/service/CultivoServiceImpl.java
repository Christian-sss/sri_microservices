package sri.microservices.cultivos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sri.microservices.cultivos.dto.CultivoRequest;
import sri.microservices.cultivos.dto.CultivoResponse;
import sri.microservices.cultivos.model.Cultivo;
import sri.microservices.cultivos.repository.CultivoRepository;

import java.util.List;

@Service
public class CultivoServiceImpl implements CultivoService {

    private final CultivoRepository cultivoRepository;

    public CultivoServiceImpl(CultivoRepository cultivoRepository) {
        this.cultivoRepository = cultivoRepository;
    }

    @Override
    public List<CultivoResponse> listarTodos() {
        return cultivoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<CultivoResponse> listarActivos() {
        return cultivoRepository.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    @Override
    public List<CultivoResponse> listarInactivos() {
        return cultivoRepository.findByActivoFalse().stream().map(this::toResponse).toList();
    }

    @Override
    public CultivoResponse obtenerPorId(Integer id) {
        Cultivo cultivo = cultivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        return toResponse(cultivo);
    }

    @Override
    @Transactional
    public CultivoResponse crear(CultivoRequest request) {
        validar(request);
        validarNombreUnico(request.nombre(), null);

        Cultivo cultivo = new Cultivo();
        aplicarDatos(cultivo, request);
        cultivo.setActivo(true);
        return toResponse(cultivoRepository.save(cultivo));
    }

    @Override
    @Transactional
    public CultivoResponse actualizar(Integer id, CultivoRequest request) {
        validar(request);
        validarNombreUnico(request.nombre(), id);

        Cultivo cultivo = cultivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        aplicarDatos(cultivo, request);
        return toResponse(cultivoRepository.save(cultivo));
    }

    @Override
    @Transactional
    public void toggleEstado(Integer id) {
        Cultivo cultivo = cultivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        cultivo.setActivo(!Boolean.TRUE.equals(cultivo.getActivo()));
        cultivoRepository.save(cultivo);
    }

    private void aplicarDatos(Cultivo cultivo, CultivoRequest request) {
        cultivo.setNombre(request.nombre().trim());
        cultivo.setHumedadMinOptima(request.humedadMinOptima());
        cultivo.setHumedadMaxOptima(request.humedadMaxOptima());
        cultivo.setDuracionRiegoMinutos(request.duracionRiegoMinutos());
        cultivo.setTratoRecomendado(request.tratoRecomendado());
    }

    private void validar(CultivoRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cultivo es obligatorio.");
        }

        if (request.nombre().matches(".*\\d.*")) {
            throw new IllegalArgumentException("El nombre del cultivo no puede contener numeros.");
        }

        if (request.humedadMinOptima() == null || request.humedadMaxOptima() == null) {
            throw new IllegalArgumentException("La humedad minima y maxima son obligatorias.");
        }

        if (request.duracionRiegoMinutos() == null || request.duracionRiegoMinutos() <= 0) {
            throw new IllegalArgumentException("La duracion de riego debe ser mayor a 0 minutos.");
        }

        if (request.humedadMinOptima() < 0
                || request.humedadMinOptima() > 100
                || request.humedadMaxOptima() < 0
                || request.humedadMaxOptima() > 100) {
            throw new IllegalArgumentException("La humedad debe estar entre 0 y 100.");
        }

        if (request.humedadMinOptima() >= request.humedadMaxOptima()) {
            throw new IllegalArgumentException("La humedad minima debe ser menor que la maxima.");
        }
    }

    private void validarNombreUnico(String nombre, Integer cultivoId) {
        String nombreNormalizado = nombre.trim();
        boolean nombreExistente = cultivoId == null
                ? cultivoRepository.existsByNombreIgnoreCase(nombreNormalizado)
                : cultivoRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, cultivoId);

        if (nombreExistente) {
            throw new IllegalArgumentException("Ya existe un perfil de cultivo con ese nombre.");
        }
    }

    private CultivoResponse toResponse(Cultivo cultivo) {
        return new CultivoResponse(
                cultivo.getId(),
                cultivo.getNombre(),
                cultivo.getHumedadMinOptima(),
                cultivo.getHumedadMaxOptima(),
                cultivo.getDuracionRiegoMinutos(),
                cultivo.getTratoRecomendado(),
                cultivo.getActivo()
        );
    }
}
