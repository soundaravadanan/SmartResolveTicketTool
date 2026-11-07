import { useEffect, useState } from 'react'
import './App.css'

const emptyForm = {
  title: '',
  description: '',
}

const statusOptions = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'CLOSED']

async function getApiError(response, fallbackMessage) {
  try {
    const body = await response.json()
    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message
    }
  } catch {
    return fallbackMessage
  }
  return fallbackMessage
}

function App() {
  const [tickets, setTickets] = useState([])
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [teams, setTeams] = useState([])
  const [assignmentDrafts, setAssignmentDrafts] = useState({})
  const [commentDrafts, setCommentDrafts] = useState({})
  const [resolutionDrafts, setResolutionDrafts] = useState({})
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  function showError(err) {
    setError(err.message)
  }

  async function loadData() {
    const [ticketsResponse, statsResponse, usersResponse, teamsResponse] = await Promise.all([
      fetch('/api/tickets'),
      fetch('/api/dashboard/stats'),
      fetch('/api/users'),
      fetch('/api/teams'),
    ])

    if (!ticketsResponse.ok || !statsResponse.ok || !usersResponse.ok || !teamsResponse.ok) {
      throw new Error('Unable to load SmartResolve data.')
    }

    const ticketData = await ticketsResponse.json()
    const statsData = await statsResponse.json()
    const userData = await usersResponse.json()
    const teamData = await teamsResponse.json()

    setTickets(ticketData)
    setStats(statsData)
    setUsers(userData)
    setTeams(teamData)
  }

  useEffect(() => {
    loadData().catch(showError)
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')

    try {
      const response = await fetch('/api/tickets', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(form),
      })

      if (!response.ok) {
        throw new Error(await getApiError(response, 'Ticket could not be created.'))
      }

      setForm(emptyForm)
      await loadData()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  function getAssignment(ticket) {
    return assignmentDrafts[ticket.id] ?? {
      assigneeId: ticket.assignee?.id ?? '',
      teamId: ticket.team?.id ?? '',
    }
  }

  function updateAssignment(ticket, changes) {
    setAssignmentDrafts((drafts) => ({
      ...drafts,
      [ticket.id]: {
        ...(drafts[ticket.id] ?? {
          assigneeId: ticket.assignee?.id ?? '',
          teamId: ticket.team?.id ?? '',
        }),
        ...changes,
      },
    }))
  }

  async function assignTicket(ticket) {
    const assignment = getAssignment(ticket)
    if (!assignment.assigneeId || !assignment.teamId) {
      setError('Please select both an assignee and a support team.')
      return
    }

    try {
      const response = await fetch(`/api/tickets/${ticket.id}/assign`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(assignment),
      })

      if (!response.ok) {
        throw new Error(await getApiError(response, 'Assignment could not be saved.'))
      }

      setAssignmentDrafts((drafts) => {
        const nextDrafts = { ...drafts }
        delete nextDrafts[ticket.id]
        return nextDrafts
      })
      await loadData()
    } catch (err) {
      setError(err.message)
    }
  }

  async function updateTicketStatus(ticketId, status) {
    try {
      const response = await fetch(`/api/tickets/${ticketId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })

      if (!response.ok) {
        throw new Error(await getApiError(response, 'Status update failed.'))
      }
      await loadData()
    } catch (err) {
      setError(err.message)
    }
  }

  async function submitTicketNote(ticket, kind) {
    const drafts = kind === 'resolution' ? resolutionDrafts : commentDrafts
    const message = drafts[ticket.id]?.trim()
    if (!message) {
      setError(kind === 'resolution' ? 'Please add resolution notes.' : 'Please enter a comment.')
      return
    }

    try {
      const response = await fetch(
        `/api/tickets/${ticket.id}/${kind === 'resolution' ? 'resolve' : 'comments'}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message }),
        },
      )

      if (!response.ok) {
        throw new Error(await getApiError(response, 'Ticket update failed.'))
      }

      const updateDrafts = kind === 'resolution' ? setResolutionDrafts : setCommentDrafts
      updateDrafts((current) => ({ ...current, [ticket.id]: '' }))
      await loadData()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">FR Channel Journey</p>
          <h1>SmartResolve Ticket Console</h1>
        </div>
        <div className="badge">AI-assisted support</div>
      </header>

      {error ? <div className="alert">{error}</div> : null}

      <section className="stats-grid">
        <div className="stat-card">
          <span>Total</span>
          <strong>{stats?.total ?? 0}</strong>
        </div>
        <div className="stat-card">
          <span>Open</span>
          <strong>{stats?.open ?? 0}</strong>
        </div>
        <div className="stat-card">
          <span>Assigned</span>
          <strong>{stats?.assigned ?? 0}</strong>
        </div>
        <div className="stat-card">
          <span>In progress</span>
          <strong>{stats?.inProgress ?? 0}</strong>
        </div>
        <div className="stat-card">
          <span>Resolved</span>
          <strong>{stats?.resolved ?? 0}</strong>
        </div>
      </section>

      <section className="layout">
        <form className="ticket-form" onSubmit={handleSubmit}>
          <h2>New support request</h2>

          <label>
            Title
            <input
              value={form.title}
              onChange={(event) =>
                setForm((current) => ({ ...current, title: event.target.value }))
              }
              placeholder="Password reset or API outage"
              required
            />
          </label>

          <label>
            Description
            <textarea
              value={form.description}
              onChange={(event) =>
                setForm((current) => ({ ...current, description: event.target.value }))
              }
              placeholder="Describe the issue, impact, and when it started"
              rows={8}
              required
            />
          </label>

          <button type="submit" disabled={saving}>
            {saving ? 'Submitting...' : 'Create ticket'}
          </button>
        </form>

        <div className="tickets-panel">
          <div className="panel-header">
            <h2>Recent tickets</h2>
            <span>{tickets.length} active</span>
          </div>

          <div className="ticket-list">
            {tickets.map((ticket) => (
              <article key={ticket.id} className="ticket-card-item">
                <div className="ticket-head">
                  <h3>{ticket.title}</h3>
                  <span className={`pill priority-${ticket.priority.toLowerCase()}`}>
                    {ticket.priority}
                  </span>
                </div>

                <p className="meta">
                  {ticket.category} • {ticket.status}
                </p>
                <p className="summary">{ticket.aiSummary}</p>

                <div className="detail-block">
                  <strong>Suggested response</strong>
                  <p>{ticket.aiSuggestedResponse}</p>
                </div>

                <div className="detail-block">
                  <strong>Troubleshooting</strong>
                  <ul>
                    {ticket.aiTroubleshooting.split('\n').map((step) => (
                      <li key={step}>{step}</li>
                    ))}
                  </ul>
                </div>

                {ticket.relatedResolvedTickets?.length > 0 && (
                  <div className="detail-block">
                    <strong>Similar resolved tickets used for AI context</strong>
                    {ticket.relatedResolvedTickets.map((relatedTicket) => (
                      <p key={relatedTicket.id}>
                        <b>{relatedTicket.title}:</b> {relatedTicket.resolution}
                      </p>
                    ))}
                  </div>
                )}

                <div className="detail-block">
                  <strong>Comments and resolution notes</strong>
                  {ticket.comments.map((comment) => (
                    <p key={comment.id}>
                      <b>{comment.type === 'RESOLUTION' ? 'Resolution' : comment.author.name}:</b>{' '}
                      {comment.message}
                    </p>
                  ))}
                  {ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' && (
                    <>
                      <form
                        className="note-form"
                        onSubmit={(event) => {
                          event.preventDefault()
                          submitTicketNote(ticket, 'comment')
                        }}
                      >
                        <textarea
                          aria-label={`Comment on ${ticket.title}`}
                          placeholder="Add a ticket comment"
                          rows={2}
                          value={commentDrafts[ticket.id] ?? ''}
                          onChange={(event) =>
                            setCommentDrafts((drafts) => ({
                              ...drafts,
                              [ticket.id]: event.target.value,
                            }))
                          }
                        />
                        <button type="submit">Add comment</button>
                      </form>
                      <form
                        className="note-form"
                        onSubmit={(event) => {
                          event.preventDefault()
                          submitTicketNote(ticket, 'resolution')
                        }}
                      >
                        <textarea
                          aria-label={`Resolution notes for ${ticket.title}`}
                          placeholder="Document the fix before resolving"
                          rows={2}
                          value={resolutionDrafts[ticket.id] ?? ''}
                          onChange={(event) =>
                            setResolutionDrafts((drafts) => ({
                              ...drafts,
                              [ticket.id]: event.target.value,
                            }))
                          }
                        />
                        <button type="submit">Resolve with notes</button>
                      </form>
                    </>
                  )}
                </div>

                <div className="assignment-box">
                  <div className="assignment-row">
                    <label>
                      Assignee
                      <select
                        value={getAssignment(ticket).assigneeId}
                        onChange={(event) =>
                          updateAssignment(ticket, { assigneeId: event.target.value })
                        }
                      >
                        <option value="">Select assignee</option>
                        {users.filter((user) => user.role === 'SUPPORT_ENGINEER').map((user) => (
                          <option key={user.id} value={user.id}>
                            {user.name}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label>
                      Team
                      <select
                        value={getAssignment(ticket).teamId}
                        onChange={(event) =>
                          updateAssignment(ticket, { teamId: event.target.value })
                        }
                      >
                        <option value="">Select team</option>
                        {teams.map((team) => (
                          <option key={team.id} value={team.id}>
                            {team.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                  <button
                    type="button"
                    className="assign-button"
                    disabled={!getAssignment(ticket).assigneeId || !getAssignment(ticket).teamId}
                    onClick={() => assignTicket(ticket)}
                  >
                    Assign ticket
                  </button>

                  <div className="assignment-row assignment-row-status">
                    <label>
                      Status
                      <select
                        value={ticket.status}
                        onChange={(event) => updateTicketStatus(ticket.id, event.target.value)}
                      >
                        {ticket.status === 'RESOLVED' && (
                          <option value="RESOLVED" disabled>
                            RESOLVED
                          </option>
                        )}
                        {ticket.status === 'CLOSED' && (
                          <option value="CLOSED" disabled>
                            CLOSED
                          </option>
                        )}
                        {statusOptions.map((status) => (
                          <option key={status} value={status}>
                            {status}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

export default App
