meta:
  id: insidious_index_parser
seq:
  - id: index_file_count
    type: u4be
  - id: index_files
    type: indexed_file
    repeat: expr
    repeat-expr: index_file_count
  - id: union_value_id_index_len
    type: u4be
  - id: union_value_id_index
    size: union_value_id_index_len
  - id: union_probe_id_index_len
    type: u4be
  - id: union_probe_id_index
    size: union_probe_id_index_len
  - id: end_time
    type: u8be
types:
  indexed_file:
    seq:
      - id: file_path
        type: str_with_len
      - id: thread_id
        type: u8be
      - id: value_id_index_len
        type: u4be
      - id: value_id_index
        size: value_id_index_len
      - id: probe_id_index_len
        type: u4be
      - id: probe_id_index
        size: probe_id_index_len
  str_with_len:
    seq:
      - id: len
        type: u4be
      - id: value
        size: len
