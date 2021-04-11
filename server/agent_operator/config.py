import related

@related.immutable
class AgentDescriptor:
    name = related.StringField(repr=True)
    address = related.StringField(repr=True)
    port = related.IntegerField(repr=True)

@related.immutable
class DescriptorSet:
    agents = related.MappingField(AgentDescriptor, "name")

    def __iter__(self):
        return self.agents.values().__iter__()
