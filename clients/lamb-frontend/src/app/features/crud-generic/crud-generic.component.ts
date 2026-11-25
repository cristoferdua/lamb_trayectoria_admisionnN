import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { CRUD_BY_SLUG } from './cruds.registry';
import { CrudDef, FieldDef } from './crud.types';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-crud-generic',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './crud-generic.component.html',
  styleUrl: './crud-generic.component.scss'
})
export class CrudGenericComponent {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private location = inject(Location);
  private router = inject(Router);

  back() { this.location.back(); }

  def = signal<CrudDef | null>(null);
  items = signal<any[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  form = signal<Record<string, any>>({});

  // Estado del buscador RENIEC embebido
  reniecDni = signal('');
  reniecLoading = signal(false);
  reniecResult = signal<{nombres: string|null, apellidoPaterno: string|null, apellidoMaterno: string|null, nombreCompleto: string|null, encontrado: boolean, fuente: string} | null>(null);
  reniecError = signal<string | null>(null);

  listedFields = computed<FieldDef[]>(() => (this.def()?.fields ?? []).filter(f => f.listed));
  editableFields = computed<FieldDef[]>(() => (this.def()?.fields ?? []).filter(f => f.editable !== false));

  constructor() {
    this.route.paramMap.subscribe(p => {
      const slug = p.get('slug');
      const d = slug ? CRUD_BY_SLUG.get(slug) : null;
      this.def.set(d ?? null);
      if (d) { this.load(); this.resetForm(); }
    });
  }

  load() {
    const d = this.def(); if (!d) return;
    this.loading.set(true); this.error.set(null);
    this.api.list<any>(d.path).subscribe({
      next: r => { this.items.set(Array.isArray(r) ? r : []); this.loading.set(false); },
      error: e => { this.error.set(this.msg(e)); this.loading.set(false); }
    });
  }

  resetForm() {
    const d = this.def(); if (!d) return;
    const empty: Record<string, any> = {};
    for (const field of d.fields) {
      if (field.editable === false) continue;
      empty[field.key] = field.default ?? (field.type === 'boolean' ? false : field.type === 'number' || field.type === 'decimal' ? null : '');
    }
    this.form.set(empty);
    this.editingId.set(null);
  }

  openCreate() {
    const d = this.def();
    if (d?.createVia) {
      this.router.navigateByUrl(d.createVia);
      return;
    }
    this.resetForm();
    this.resetReniec();
    this.showForm.set(true);
  }

  resetReniec() {
    this.reniecDni.set('');
    this.reniecResult.set(null);
    this.reniecError.set(null);
    this.reniecLoading.set(false);
  }

  openEdit(row: any) {
    const d = this.def(); if (!d) return;
    const data: Record<string, any> = {};
    for (const field of d.fields) {
      data[field.key] = row[field.key] ?? null;
    }
    this.form.set(data);
    this.editingId.set(row.id ?? null);
    this.showForm.set(true);
  }

  buscarReniec() {
    const dni = (this.reniecDni() || '').trim();
    if (!/^\d{8}$/.test(dni)) {
      this.reniecError.set('DNI debe tener 8 dígitos.');
      this.reniecResult.set(null);
      return;
    }
    this.reniecError.set(null);
    this.reniecLoading.set(true);
    this.api.get<any>(`/api/v1/admision/reniec/${dni}`).subscribe({
      next: r => {
        this.reniecResult.set(r);
        this.reniecLoading.set(false);
        if (!r.encontrado) this.reniecError.set(`No se encontraron datos (${r.fuente}).`);
      },
      error: e => {
        this.reniecLoading.set(false);
        this.reniecError.set(`Error RENIEC: ${e.status ?? ''} ${e.message ?? ''}`);
      }
    });
  }

  save() {
    const d = this.def(); if (!d) return;
    const body = { ...this.form() };
    // quitar campos virtuales (empiezan con __) y vacios
    for (const k of Object.keys(body)) {
      if (k.startsWith('__') || body[k] === '' || body[k] === null) delete body[k];
    }
    const id = this.editingId();
    const req = id ? this.api.put<any>(`${d.path}/${id}`, body) : this.api.post<any>(d.path, body);
    req.subscribe({
      next: saved => {
        if (id) {
          this.items.update(a => a.map(x => x.id === id ? saved : x));
        } else {
          this.items.update(a => [saved, ...a]);
        }
        this.showForm.set(false);
        this.error.set(null);
      },
      error: e => this.error.set(this.msg(e))
    });
  }

  updateField(key: string, value: any) {
    this.form.update(f => ({ ...f, [key]: value }));
  }

  download(row: any) {
    const d = this.def(); if (!d?.rowDownload) return;
    const dl = d.rowDownload;
    const path = dl.path.replace('{id}', row.id);
    const codigo = dl.codeField ? (row[dl.codeField] ?? row.id) : row.id;
    const filename = dl.filename.replace('{id}', row.id).replace('{codigo}', codigo);
    const a = document.createElement('a');
    a.href = `${environment.apiUrl}${path}`;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  remove(row: any) {
    const d = this.def(); if (!d) return;
    if (!confirm(`¿Eliminar registro #${row.id}?`)) return;
    this.api.delete<void>(`${d.path}/${row.id}`).subscribe({
      next: () => {
        this.items.update(a => a.filter(x => x.id !== row.id));
        this.error.set(null);
      },
      error: e => this.error.set(this.msg(e))
    });
  }

  fmtCell(row: any, field: FieldDef) {
    const v = row[field.key];
    if (v === null || v === undefined || v === '') return '—';
    if (typeof v === 'object') return JSON.stringify(v).slice(0, 40);
    return String(v);
  }

  private msg(e: any): string {
    const s = e.status ?? '';
    const m = e.error?.message ?? e.error?.detail ?? e.message ?? 'Error';
    return `${s} ${m}`.trim();
  }
}
