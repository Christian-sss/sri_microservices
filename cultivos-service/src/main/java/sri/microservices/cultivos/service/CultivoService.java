package sri.microservices.cultivos.service;

import sri.microservices.cultivos.dto.CultivoRequest;
import sri.microservices.cultivos.dto.CultivoResponse;

import java.util.List;

public interface CultivoService {

    List<CultivoResponse> listarTodos();

    List<CultivoResponse> listarActivos();

    List<CultivoResponse> listarInactivos();

    CultivoResponse obtenerPorId(Integer id);

    CultivoResponse crear(CultivoRequest request);

    CultivoResponse actualizar(Integer id, CultivoRequest request);

    void toggleEstado(Integer id);
}
