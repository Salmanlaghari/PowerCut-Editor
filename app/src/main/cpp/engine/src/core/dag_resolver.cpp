// =============================================================================
// PowerCut Pro 2027 8K — DAG resolver impl (kept working backend)
// File: src/core/dag_resolver.cpp
// =============================================================================
#include "powercut/core/dag_resolver.h"
#include <stdexcept>
#include <unordered_map>
#include <unordered_set>

namespace powercut::core {

namespace {
enum class Color { WHITE, GRAY, BLACK };
void visit(const std::string& id,
           const std::unordered_map<std::string, const DAGNode*>& map,
           std::unordered_map<std::string, Color>& color,
           std::vector<std::string>& out) {
    auto it = color.find(id);
    if (it == color.end()) throw std::runtime_error("DAG: missing node " + id);
    if (it->second == Color::GRAY) throw std::runtime_error("DAG: cycle detected at " + id);
    if (it->second == Color::BLACK) return;
    it->second = Color::GRAY;
    auto node_it = map.find(id);
    if (node_it != map.end()) {
        for (const auto& dep : node_it->second->deps) visit(dep, map, color, out);
    }
    it->second = Color::BLACK;
    out.push_back(id);
}
} // namespace

std::vector<std::string> DAGResolver::resolve(const std::vector<DAGNode>& nodes) {
    std::unordered_map<std::string, const DAGNode*> map;
    std::unordered_map<std::string, Color> color;
    for (const auto& n : nodes) {
        if (!map.insert({n.id, &n}).second)
            throw std::runtime_error("DAG: duplicate node id " + n.id);
        color[n.id] = Color::WHITE;
    }
    std::vector<std::string> out;
    out.reserve(nodes.size());
    for (const auto& n : nodes) visit(n.id, map, color, out);
    return out;
}

bool DAGResolver::validate(const std::vector<DAGNode>& nodes) {
    try { (void)resolve(nodes); return true; }
    catch (...) { return false; }
}

} // namespace powercut::core
