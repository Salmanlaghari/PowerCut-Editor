// =============================================================================
// PowerCut Pro 2027 8K — effect DAG resolver (kept working backend)
// File: include/powercut/core/dag_resolver.h
// =============================================================================
#pragma once
#include "powercut/core/types.h"
#include <vector>
#include <string>

namespace powercut::core {

// Resolves the topological order of a DAG of effect nodes. The original
// implementation is preserved — this is the kept-working contract used by
// AI Hub, Filters, Effects, 3D, Chroma, VFX screens (P4).
class DAGResolver {
public:
    // Returns node ids in render order. Throws std::runtime_error on cycles.
    static std::vector<std::string> resolve(const std::vector<DAGNode>& nodes);

    // Validates that every dep id exists and the graph is acyclic.
    static bool validate(const std::vector<DAGNode>& nodes);
};

} // namespace powercut::core
