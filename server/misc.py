
def mark_object(obj, marker: str):
    if not is_marked(obj):
        obj.markers = set()
    obj.markers.add(marker)

def is_marked(obj, marker: str = None):
    if not (hasattr(obj, "markers") and
            isinstance(obj.markers, set)):
        return False
    elif not marker:
        return True

    return marker in obj.markers

def clean_object(obj, marker: str):
    if is_marked(obj):
        if marker:
            obj.markers.remove(marker)
        else:
            del obj.markers
